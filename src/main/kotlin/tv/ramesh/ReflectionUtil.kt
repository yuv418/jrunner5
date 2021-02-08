package tv.ramesh;

import org.joor.*
import org.joor.Reflect.*
import java.lang.reflect.ReflectPermission
import java.security.Permission
import java.util.Arrays;
import org.apache.commons.lang3.exception.ExceptionUtils
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

    fun evalProblemSolution(inputFunction: String, inputFunctionName: String, solutionFunction: String, functionArgs: ArrayList<String>): tv.ramesh.Response {
        // val functionFindRegex = Regex("""(public \w+ )([a-z]\w*)(\s*\(.+\) \{)""") // Replace the solution so the input function can't just call solution() and cheat
        // val inputFunctionName = functionFindRegex.findAll(inputFunction).map { it.groupValues[2] }.joinToString()

        var runInputs = ""; // Instantiate objects earlier
        var runSolutionInputs = "Object solutionFnOutput = new Object();"; // Instantiate objects earlier

        // runInputs += "tv.ramesh.RunResultType finalResultType = tv.ramesh.RunResultType.Success;"
        for ((i, arg) in functionArgs.withIndex()) {
            runInputs += "public Object runProblemInput$i() {"
            runInputs += "Object inputFnOutput = new Object();"
            runInputs += "try {\n"
            runInputs += "inputFnOutput = $inputFunctionName($arg);\n" // Try user input function
            runInputs += "inputFnOutputs.add(inputFnOutput.toString());\n" // Add to input function output list
            runInputs += "}\n"
            runInputs += "catch (Exception e) {\n" // Means that something went wrong, add exception information instead of output information
            // runInputs += "System.out.println(e);\n" // Means that something went wrong, add exception information instead of output information
            runInputs += "inputFnOutputs.add(e.toString());\n" // Add to input function output list
            runInputs += "runResultType = tv.ramesh.RunResultType.RuntimeError;\n" // Add to output function output list
            runInputs += "}\n"
            runInputs += "return inputFnOutput;"
            runInputs += "}"

            // Solution outputs
            runSolutionInputs += "public Object runProblemInput$i() {"
            runSolutionInputs += "Object solutionFnOutput = new Object();"
            runSolutionInputs += "try {\n"
            runSolutionInputs += "solutionFnOutput = solution($arg);\n" // Try solution function
            runSolutionInputs += "inputFnOutputs.add(solutionFnOutput.toString());\n\n" // Add to solution function output list
            runSolutionInputs += "}\n"
            runSolutionInputs += "catch (Exception e) {\n" // Means that something went wrong, add exception information instead of output information
            runSolutionInputs += "inputFnOutputs.add(e.toString());\n" // Add to output function output list
            runSolutionInputs += "runResultType = tv.ramesh.RunResultType.RuntimeError;\n" // Add to output function output list
            runSolutionInputs += "}\n"
            runSolutionInputs += "return solutionFnOutput;"
            runSolutionInputs += "}"
            // runSolutionInputs += "matches.add(inputFnOutput.equals(solutionFnOutput));\n" // Compare both of them
        }

        val sharedJava = """
package sandboxed;
import java.util.ArrayList;

public class JavaWrappedClass {
    private ArrayList<String> inputFnOutputs;
    private tv.ramesh.RunResultType runResultType;

    public JavaWrappedClass() {
        inputFnOutputs = new ArrayList<>();
        runResultType = tv.ramesh.RunResultType.Success;
    }

    public ArrayList<String> getOutputs() {
        return this.inputFnOutputs;
    }

    public tv.ramesh.RunResultType getRunResultType() {
        return this.runResultType;
    }

"""

        val problemJava = sharedJava + "$runInputs $inputFunction}"
        // println(problemJava)

        val solutionJava = sharedJava + "$runSolutionInputs $solutionFunction}"

        // println("DEBUG! Compiled java code is \n$java")


        val problemOut = compileRunJava(problemJava, functionArgs)
        val solutionOut = compileRunJava(solutionJava, functionArgs)

        var runResultType = RunResultType.Success

        var matches: ArrayList<Boolean> = arrayListOf()
        for ((i, oObj) in problemOut.second.withIndex()) {
            matches.add(oObj.equals(solutionOut.second.get(i)))
        }

        if (problemOut.first.first != null) {
            if (solutionOut.first.first == null) {
                // Merge the two
                return Response(solutionOut.third, arrayListOf(problemOut.first.first!!), matches, problemOut.first.second)
            }
            else if (solutionOut.first.first != null) {
                return Response(arrayListOf(solutionOut.first.first!!), arrayListOf(problemOut.first.first!!), matches, problemOut.first!!.second)
            }
        }

        if (solutionOut.first.first != null) {
            if (problemOut.first.first == null) {
                return Response(arrayListOf(problemOut.first.first!!), problemOut.third, matches, problemOut.first.second)
            }
            else if (problemOut.first != null) {
                return Response(arrayListOf(problemOut.first.first!!), arrayListOf(problemOut.first.first!!), matches, problemOut.first!!.second)
            }
        }

        if (solutionOut.first.second != RunResultType.Success || problemOut.first.second != RunResultType.Success) { // Compiler error would've been exhausted before this
            runResultType = RunResultType.RuntimeError
        }

        return Response(solutionOut.third, problemOut.third, matches, runResultType)

    }

    fun compileRunJava(java: String, functionArgs: ArrayList<String>): Triple<Pair<String?, RunResultType>, ArrayList<Any>, ArrayList<String>> {

        var ref: Reflect;
        var aggregatedObjects: ArrayList<Any> = arrayListOf();

        try {
            ref = Reflect.compile("sandboxed.JavaWrappedClass", java).create();
        }
        catch (e: ReflectException) {
            return Triple(Pair(e.toString(), RunResultType.CompilerError), arrayListOf(), arrayListOf())
        }

        val classInst: Any = ref.get()

        for (i in functionArgs.indices) {

            try {
                val returnedObj = classInst.javaClass.getMethod("runProblemInput$i").invoke(classInst) as Any;
                aggregatedObjects.add(returnedObj)
            }
            catch (e: Exception) {
                val rootCause = ExceptionUtils.getRootCause(e)
                return Triple(Pair(rootCause.toString(), RunResultType.RuntimeError), arrayListOf(), arrayListOf())
            }
        }
        val outputs = on(classInst).call("getOutputs").get() as ArrayList<String>
        val rrType = on(classInst).call("getRunResultType").get() as RunResultType

        return Triple(Pair(null, rrType), aggregatedObjects, outputs)
    }
}
