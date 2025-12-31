package deltazero.amarok.xposed.hooks

interface IFrameworkHook {
    fun load()
    fun unload()
    fun onConfigChanged() {}
}
