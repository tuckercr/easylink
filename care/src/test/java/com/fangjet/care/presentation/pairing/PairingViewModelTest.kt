package com.fangjet.care.presentation.pairing

import com.fangjet.care.data.pairing.CarePairingRepository
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingViewModelTest {

    private val repository = mockk<CarePairingRepository>()
    private val viewModel = PairingViewModel(repository)

    @Test
    fun `strips non-digits so a pasted formatted code still works`() {
        viewModel.onCodeChanged("123-456")
        assertEquals("123456", viewModel.uiState.value.code)

        viewModel.onCodeChanged("123 456")
        assertEquals("123456", viewModel.uiState.value.code)
    }

    @Test
    fun `caps input at six digits`() {
        viewModel.onCodeChanged("1234567890")
        assertEquals("123456", viewModel.uiState.value.code)
    }

    @Test
    fun `preserves leading zeros`() {
        viewModel.onCodeChanged("000123")
        assertEquals("000123", viewModel.uiState.value.code)
    }

    @Test
    fun `cannot submit until six digits are entered`() {
        viewModel.onCodeChanged("12345")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onCodeChanged("123456")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `submitting a short code surfaces an error instead of proceeding`() {
        viewModel.onCodeChanged("12")
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals("Enter all 6 digits.", state.error)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `editing clears a previous error`() {
        viewModel.onCodeChanged("12")
        viewModel.onSubmit()
        assertEquals("Enter all 6 digits.", viewModel.uiState.value.error)

        viewModel.onCodeChanged("123")
        assertNull(viewModel.uiState.value.error)
    }
}
