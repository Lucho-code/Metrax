package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MeasurementMode
import com.example.data.model.Point3D
import com.example.ui.viewmodel.MeasurementViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeasurementViewModelTest {

    private lateinit var viewModel: MeasurementViewModel

    @Before
    fun setUp() {
        viewModel = MeasurementViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `distance mode sums consecutive segments`() {
        viewModel.setMode(MeasurementMode.DISTANCE)
        viewModel.addPoint(Point3D(0f, 0f, 0f))
        viewModel.addPoint(Point3D(3f, 0f, 0f))
        viewModel.addPoint(Point3D(3f, 0f, 4f))

        // 3m + 4m = 7m total path length
        assertEquals(7.0, viewModel.calculateCurrentValue(), 0.001)
    }

    @Test
    fun `undo removes last point`() {
        viewModel.setMode(MeasurementMode.DISTANCE)
        viewModel.addPoint(Point3D(0f, 0f, 0f))
        viewModel.addPoint(Point3D(1f, 0f, 0f))
        viewModel.undoLastPoint()

        assertEquals(1, viewModel.points.value.size)
    }

    @Test
    fun `area mode computes ordered polygon area`() {
        viewModel.setMode(MeasurementMode.AREA)
        viewModel.addPoint(Point3D(0f, 0f, 0f))
        viewModel.addPoint(Point3D(2f, 0f, 0f))
        viewModel.addPoint(Point3D(2f, 0f, 2f))
        viewModel.addPoint(Point3D(0f, 0f, 2f))

        assertEquals(4.0, viewModel.calculateCurrentValue(), 0.01)
    }

    @Test
    fun `volume mode footprint area ignores point cloud scan order`() {
        viewModel.setMode(MeasurementMode.VOLUME)
        // Scrambled scan order for a 2x2 footprint, as ARCore point cloud
        // samples would arrive -- this is the exact scenario that used to
        // produce a garbage self-intersecting-polygon area.
        viewModel.addPoint(Point3D(2f, 0f, 2f))
        viewModel.addPoint(Point3D(0f, 0f, 0f))
        viewModel.addPoint(Point3D(0f, 0f, 2f))
        viewModel.addPoint(Point3D(2f, 0f, 0f))
        viewModel.setHeightMeters(3.0)

        // 4 m^2 footprint * 3 m height = 12 m^3
        assertEquals(12.0, viewModel.calculateCurrentValue(), 0.05)
    }

    @Test
    fun `setCustomDensity overrides selected material density and keeps its name`() {
        val originalName = viewModel.selectedMaterial.value.name
        viewModel.setCustomDensity(1234f)

        assertEquals(originalName, viewModel.selectedMaterial.value.name)
        assertEquals(1234f, viewModel.selectedMaterial.value.densityKgPerM3, 0.001f)
    }

    @Test
    fun `setCustomDensity ignores non-positive values`() {
        val original = viewModel.selectedMaterial.value
        viewModel.setCustomDensity(0f)
        viewModel.setCustomDensity(-5f)

        assertEquals(original, viewModel.selectedMaterial.value)
    }

    @Test
    fun `material presets use Spanish names`() {
        val names = viewModel.availableMaterials.map { it.name }
        assertTrue(names.contains("Madera"))
        assertTrue(names.contains("Hormigón armado (RCC)"))
        assertTrue(names.none { it == "Wood" || it == "RCC" || it == "Steel" })
    }

    @Test
    fun `switching mode clears points`() {
        viewModel.setMode(MeasurementMode.DISTANCE)
        viewModel.addPoint(Point3D(0f, 0f, 0f))
        viewModel.setMode(MeasurementMode.AREA)

        assertEquals(0, viewModel.points.value.size)
    }
}
