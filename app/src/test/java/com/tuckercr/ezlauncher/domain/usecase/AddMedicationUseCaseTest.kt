package com.tuckercr.ezlauncher.domain.usecase

import com.tuckercr.ezlauncher.domain.model.Medication
import com.tuckercr.ezlauncher.domain.model.MedicationColor
import com.tuckercr.ezlauncher.domain.repository.MedicationRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class AddMedicationUseCaseTest {

    private lateinit var repository: MedicationRepository
    private lateinit var useCase: AddMedicationUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = AddMedicationUseCase(repository)
    }

    private fun validMedication(
        name: String = "Metformin",
        reminderTimes: List<LocalTime> = listOf(LocalTime.of(8, 0)),
        activeDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
    ) = Medication(
        id           = 0L,
        name         = name,
        dosage       = "500mg",
        notes        = "",
        reminderTimes = reminderTimes,
        activeDays   = activeDays,
        isActive     = true,
        color        = MedicationColor.BLUE,
    )

    @Test
    fun `returns Success and saves when medication is valid`() = runTest {
        val medication = validMedication()
        val result = useCase(medication)

        assertTrue(result is AddMedicationUseCase.Result.Success)
        coVerify(exactly = 1) { repository.saveMedication(medication) }
    }

    @Test
    fun `returns InvalidName when name is blank`() = runTest {
        val result = useCase(validMedication(name = "   "))

        assertTrue(result is AddMedicationUseCase.Result.InvalidName)
        coVerify(exactly = 0) { repository.saveMedication(any()) }
    }

    @Test
    fun `returns InvalidName when name is empty`() = runTest {
        val result = useCase(validMedication(name = ""))

        assertTrue(result is AddMedicationUseCase.Result.InvalidName)
    }

    @Test
    fun `returns NoReminderTimes when reminder list is empty`() = runTest {
        val result = useCase(validMedication(reminderTimes = emptyList()))

        assertTrue(result is AddMedicationUseCase.Result.NoReminderTimes)
        coVerify(exactly = 0) { repository.saveMedication(any()) }
    }

    @Test
    fun `returns NoActiveDays when active days is empty`() = runTest {
        val result = useCase(validMedication(activeDays = emptySet()))

        assertTrue(result is AddMedicationUseCase.Result.NoActiveDays)
        coVerify(exactly = 0) { repository.saveMedication(any()) }
    }

    @Test
    fun `name validation is checked before times and days`() = runTest {
        // All three errors could fire; InvalidName should be the result
        val medication = validMedication(
            name         = "",
            reminderTimes = emptyList(),
            activeDays   = emptySet(),
        )
        val result = useCase(medication)
        assertEquals(AddMedicationUseCase.Result.InvalidName("Medication name cannot be empty"), result)
    }

    @Test
    fun `accepts single reminder time and single active day`() = runTest {
        val result = useCase(
            validMedication(
                reminderTimes = listOf(LocalTime.NOON),
                activeDays   = setOf(DayOfWeek.FRIDAY),
            )
        )
        assertTrue(result is AddMedicationUseCase.Result.Success)
    }
}
