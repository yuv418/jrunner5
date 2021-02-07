package tv.ramesh;

import org.joor.*
import java.lang.reflect.ReflectPermission
import java.security.Permission
import org.apache.commons.lang3.exception.ExceptionUtils


class ReflectionUtil {

    fun evalProblemSolution(inputFunction: String, inputFunctionName: String, solutionFunction: String, functionArgs: Array<String>): tv.ramesh.Response {

        var runInputs = "Object inputFnOutput = new Object(); Object solutionFnOutput = new Object();"; // Instantiate objects earlier
        runInputs += "tv.ramesh.RunResultType finalResultType = tv.ramesh.RunResultType.Success;"
        for (arg in functionArgs) {
            runInputs += "try {\n"
            runInputs += "inputFnOutput = $inputFunctionName($arg);\n" // Try user input function
            runInputs += "inputFnOutputs.add(inputFnOutput.toString());\n" // Add to input function output list
            runInputs += "}\n"
            runInputs += "catch (Exception e) {\n" // Means that something went wrong, add exception information instead of output information
            runInputs += "inputFnOutputs.add(e.toString());\n" // Add to input function output list
            runInputs += "finalResultType = tv.ramesh.RunResultType.RuntimeError;\n"
            runInputs += "}\n"
            runInputs += "try {\n"
            runInputs += "solutionFnOutput = solution($arg);\n" // Try solution function
            runInputs += "solutionFnOutputs.add(solutionFnOutput.toString());\n\n" // Add to solution function output list
            runInputs += "}\n"
            runInputs += "catch (Exception e) {\n" // Means that something went wrong, add exception information instead of output information
            runInputs += "solutionFnOutputs.add(e.toString());\n" // Add to output function output list
            runInputs += "finalResultType = tv.ramesh.RunResultType.RuntimeError;\n"
            runInputs += "}\n"
            runInputs += "matches.add(inputFnOutput.equals(solutionFnOutput));\n" // Compare both of them
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


        return new tv.ramesh.Response(solutionFnOutputs, inputFnOutputs, matches, finalResultType);
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
        return classInst.javaClass.getMethod("runProblem").invoke(classInst) as Response;

    }
}
