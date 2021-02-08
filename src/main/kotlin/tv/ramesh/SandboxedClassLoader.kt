package tv.ramesh

class SandboxedClassLoader: ClassLoader() {

    override fun loadClass(name: String): Class<*> {
        println(name)
        return super.loadClass(name);
    }

}
