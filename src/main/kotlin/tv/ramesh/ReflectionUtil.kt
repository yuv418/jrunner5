package tv.ramesh;

import org.joor.*
import org.joor.Reflect.*
import java.lang.reflect.ReflectPermission
import java.security.Permission
import java.util.Arrays;
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.tuple.Triple as JavaTriple
import either.Either;
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.random.Random

class ReflectionUtil {

    private fun randomFunctionName(): String { // UNUSED
        // Generate random function names (length 24) for solution security

        val alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val numeric = "0123456789"

        var functionName = ""

        functionName += alpha[Random.nextInt(alpha.length)]

        val alphanumeric = alpha + numeric;
        repeat(23) {
            functionName += alphanumeric[Random.nextInt(alphanumeric.length)]
        }

        return functionName
    }

    fun evalProblemSolution(inputFunction: String, inputFunctionName: String, solutionFunction: String, functionArgs: ArrayList<String>?, timeout: Long): tv.ramesh.Response {
        // val functionFindRegex = Regex("""(public \w+ )([a-z]\w*)(\s*\(.+\) \{)""") // Replace the solution so the input function can't just call solution() and cheat
        // val inputFunctionName = functionFindRegex.findAll(inputFunction).map { it.groupValues[2] }.joinToString()

        var functionArgs = functionArgs;

        if (functionArgs == null) {
            functionArgs = arrayListOf();
        }


        var runInputs = ""; // Instantiate objects earlier
        var runSolutionInputs = "Object solutionFnOutput = new Object();"; // Instantiate objects earlier

        // runInputs += "tv.ramesh.RunResultType finalResultType = tv.ramesh.RunResultType.Success;"
        val runCode = { index: Int, functionName: String, arg: String -> """
public Triple<Object, tv.ramesh.OutputResultType, String> runProblemInput$index() {
    Object inputFnOutput = new Object();
    String output = "";
    tv.ramesh.OutputResultType runResultType = tv.ramesh.OutputResultType.Success;
    try {
        inputFnOutput = $functionName($arg);
        output = inputFnOutput.toString();
    }
    catch (Exception e) {
        output = e.toString();
        runResultType = tv.ramesh.OutputResultType.RuntimeError;
    }

    return Triple.of(inputFnOutput, runResultType, output);
}
""" }
        for ((i, arg) in functionArgs.withIndex()) {
            runInputs += runCode(i, inputFunctionName, arg)
            runSolutionInputs += runCode(i, "solution", arg)
        }
        // println(runInputs)

        val sharedJava = """
package sandboxed;
import java.util.ArrayList;
import org.apache.commons.lang3.tuple.Triple;

public class JavaWrappedClass {
    private ArrayList<String> inputFnOutputs;

    public JavaWrappedClass() {
        inputFnOutputs = new ArrayList<>();
    }

    public ArrayList<String> getOutputs() {
        return this.inputFnOutputs;
    }


"""

        val problemJava = sharedJava + """
        $runInputs

        $inputFunction

}"""
        // println(problemJava)

        val solutionJava = sharedJava + """

        $runSolutionInputs

        $solutionFunction

    }"""

        // println("DEBUG! Compiled java code is \n$java")


        val problemOut = compileRunJava(problemJava, functionArgs, timeout)
        val solutionOut = compileRunJava(solutionJava, functionArgs, timeout)

        var runResultType = RunResultType.Success
        var outputs: ArrayList<Output> = arrayListOf()

        // TODO there are many possibilities here. We should make a method that composes the different types together
        when (problemOut) {
            is Either.Left -> {
                runResultType = RunResultType.CompilerError
                when (solutionOut) {
                    is Either.Left -> { // both failed to compile
                        outputs = arrayListOf(
                            Output(solutionOut.value.first, OutputResultType.CompilerError, problemOut.value.first, OutputResultType.CompilerError, false)
                        )
                    }
                    is Either.Right -> { // problem failed to compile, solution compiled
                        outputs = arrayListOf(
                            Output("", OutputResultType.Success, problemOut.value.first, OutputResultType.CompilerError, false)
                        )
                    }
                }
            }
            is Either.Right -> {
                println("thing " + problemOut.value.size)

                if (problemOut.value.size > 0) {
                    println("got here")
                    for (i in 0..(problemOut.value.size - 1)) {
                        val problemTriple = problemOut.value[i]
                        when (solutionOut) {
                            is Either.Right -> {
                                val solnTriple = solutionOut.value[i]

                                var tOutput = Output(solnTriple.third, solnTriple.second, problemTriple.third, problemTriple.second, problemTriple.first.equals(solnTriple.first))
                                outputs.add(tOutput)
                            }
                            is Either.Left -> { // problem compiled, solution didn't compile
                                outputs = arrayListOf(
                                    Output(solutionOut.value.first, OutputResultType.CompilerError, problemTriple.third, problemTriple.second, false)
                                )
                                runResultType = RunResultType.CompilerError
                            }
                        }
                    }
                }
                else { // Compiler check
                    when (solutionOut) {
                        is Either.Left -> { // input compiled, solution didn't
                            outputs = arrayListOf(
                                Output(solutionOut.value.first, OutputResultType.CompilerError, "", OutputResultType.Success, false)
                            )
                            runResultType = RunResultType.CompilerError
                        }
                        else -> {}
                    }
                }
            }
        }


        return Response(runResultType, outputs)

    }

    fun compileRunJava(java: String, functionArgs: ArrayList<String>, timeout: Long): Either<Pair<String, RunResultType>, ArrayList<Triple<Any, OutputResultType, String>>> {

        var ref: Reflect;
        var aggregatedObjects: ArrayList<Triple<Any, OutputResultType, String>> = arrayListOf();

        try {
            ref = Reflect.compile("sandboxed.JavaWrappedClass", java).create();
        }
        catch (e: ReflectException) {
            return Either.Left(Pair(e.toString(), RunResultType.CompilerError))
        }


        val classInst: Any = ref.get()
        for (i in functionArgs.indices) {
            var sRun = Executors.newFixedThreadPool(1)
            var future = sRun.submit(object: Runnable {
                override fun run() {
                    try {
                        val returnedObj: JavaTriple<Any, OutputResultType, String> = on(classInst).call("runProblemInput$i").get() as JavaTriple<Any, OutputResultType, String>
                        var outputResultType = OutputResultType.Success
                        aggregatedObjects.add(Triple(returnedObj.left!!, returnedObj.middle, returnedObj.right!!))

                        /*if (returnedObj.right.contains("Exception")) {
                            outputResultType = OutputResultType.RuntimeError
                        }*/
                    }
                    catch (se: Exception) {
                        val rootCause = ExceptionUtils.getRootCause(se)
                        // Always a SecurityException (hopefully!) if we land here; there is an error handler elsewhere
                        aggregatedObjects.add(Triple(Object(), OutputResultType.SecurityError, rootCause.toString()))
                    }
                }
            })

            try {
                future.get(timeout as Long, TimeUnit.SECONDS) // Don't need to return anything
            }
            catch (te: TimeoutException) {
                future.cancel(true)
                aggregatedObjects.add(Triple(Object(), OutputResultType.TimeoutError, te.toString()))
            }
        }

        return Either.Right(aggregatedObjects)
    }

}
