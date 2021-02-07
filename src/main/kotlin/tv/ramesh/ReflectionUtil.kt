package tv.ramesh;

import org.joor.*
import java.lang.reflect.ReflectPermission
import java.security.Permission
import org.apache.commons.lang3.exception.ExceptionUtils


class ReflectionUtil {

    fun evalProblemSolution(inputFunction: String, inputFunctionName: String, solutionFunction: String, functionArgs: Array<String>): tv.ramesh.Response {

        var runInputs = "Object inputFnOutput = null; Object solutionFnOutput = null;"; // Instantiate objects earlier
        for (arg in functionArgs) {
            runInputs += "inputFnOutput = $inputFunctionName($arg);\n" // Try user input function
            runInputs += "solutionFnOutput = solution($arg);\n" // Try solution function
            runInputs += "matches.add(inputFnOutput.equals(solutionFnOutput));\n" // Compare both of them
            runInputs += "inputFnOutputs.add(inputFnOutput.toString());\n" // Add to input function output list
            runInputs += "solutionFnOutputs.add(solutionFnOutput.toString());\n\n" // Add to solution function output list
        }

        val java = """
package sandboxed;
import java.util.ArrayList;

public class JavaWrappedClass {
    public tv.ramesh.Response runProblem() {
        ArrayList<String> inputFnOutputs = new ArrayList<>();
        ArrayList<String> solutionFnOutputs = new ArrayList<>();
        ArrayList<Boolean> matches = new ArrayList<>();

        $runInputs


        return new tv.ramesh.Response(solutionFnOutputs, inputFnOutputs, matches, tv.ramesh.RunResultType.Success);
    }
    $inputFunction
    $solutionFunction
}
""".trimIndent();

        // println("DEBUG! Compiled java code is \n$java")


        var ref: Reflect;
        try {
            ref = Reflect.compile("sandboxed.JavaWrappedClass", java).create();
        }
        catch (e: ReflectException) {
            return Response(arrayListOf(e.toString()), arrayListOf(), arrayListOf(), RunResultType.CompilerError)
        }
        val classInst: Any = ref.get()

        try {
            return classInst.javaClass.getMethod("runProblem").invoke(classInst) as Response;
        }
        catch (e: Exception) {
            var cause = ExceptionUtils.getRootCause(e)

            return Response(arrayListOf(cause.toString()), arrayListOf(), arrayListOf(), RunResultType.RuntimeError)
        }

    }
}
