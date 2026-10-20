package dev.gloria.apkextractor.apps

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val apps = listOf(
        app("com.example.game", "Super Game", isSystem = false),
        app("com.example.notes", "Notes", isSystem = false),
        app("com.android.settings", "Settings", isSystem = true),
    )

    private fun app(pkg: String, label: String, isSystem: Boolean) =
        InstalledApp(pkg, label, isSystem, "/data/app/$pkg/base.apk", emptyList())

    private fun viewModel() = AppListViewModel(
        repo = object : AppRepository {
            override fun loadApps() = apps
        },
        ioDispatcher = dispatcher,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `filters out system apps by default`() {
        val labels = viewModel().state.value.apps.map { it.label }
        assertEquals(listOf("Notes", "Super Game"), labels)
    }

    @Test
    fun `showSystem true includes them`() {
        val vm = viewModel()
        vm.setShowSystem(true)
        assertEquals(
            listOf("Notes", "Settings", "Super Game"),
            vm.state.value.apps.map { it.label },
        )
    }

    @Test
    fun `query filters by label case-insensitively`() {
        val vm = viewModel()
        vm.setQuery("sUPER")
        assertEquals(listOf("Super Game"), vm.state.value.apps.map { it.label })
    }

    @Test
    fun `query matches package name`() {
        val vm = viewModel()
        vm.setQuery("example.notes")
        assertEquals(listOf("Notes"), vm.state.value.apps.map { it.label })
    }

    @Test
    fun `active extraction is kept until cleared`() {
        val vm = viewModel()
        vm.setActiveExtraction("work-1")
        vm.setQuery("notes")
        assertEquals("work-1", vm.activeExtraction.value)
        vm.setActiveExtraction(null)
        assertEquals(null, vm.activeExtraction.value)
    }
}
