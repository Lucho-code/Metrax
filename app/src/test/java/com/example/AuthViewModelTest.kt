package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AuthMode
import com.example.data.model.PasswordStrength
import com.example.ui.viewmodel.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        viewModel = AuthViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `calculatePasswordStrength classifies weak, medium, and strong passwords correctly`() {
        val weak1 = viewModel.calculatePasswordStrength("12345")
        val weak2 = viewModel.calculatePasswordStrength("short")
        val medium = viewModel.calculatePasswordStrength("password12")
        val strong = viewModel.calculatePasswordStrength("ComplexP@ssw0rd!")

        assertEquals(PasswordStrength.WEAK, weak1)
        assertEquals(PasswordStrength.WEAK, weak2)
        assertEquals(PasswordStrength.MEDIUM, medium)
        assertEquals(PasswordStrength.STRONG, strong)
    }

    @Test
    fun `validateAndRegister fails when email is invalid or passwords do not match`() {
        viewModel.onRegisterNameChanged("Lucio Rostagno")
        viewModel.onRegisterEmailChanged("invalid-email")
        viewModel.onRegisterPasswordChanged("Password123")
        viewModel.onRegisterConfirmPasswordChanged("DifferentPassword")
        viewModel.onTermsToggled(false)

        viewModel.validateAndRegister(onSuccess = {})

        val state = viewModel.registerState.value
        assertNotNull(state.emailError)
        assertNotNull(state.confirmPasswordError)
        assertNotNull(state.termsError)
        assertNull(viewModel.currentUser.value)
    }

    @Test
    fun `validateAndRegister succeeds with valid inputs`() {
        viewModel.onRegisterNameChanged("Lucio Rostagno")
        viewModel.onRegisterEmailChanged("lucio@example.com")
        viewModel.onRegisterPasswordChanged("Password123")
        viewModel.onRegisterConfirmPasswordChanged("Password123")
        viewModel.onTermsToggled(true)

        var successTriggered = false
        viewModel.validateAndRegister(onSuccess = { successTriggered = true })

        // Give simulated coroutine time if needed
        val state = viewModel.registerState.value
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.confirmPasswordError)
    }

    @Test
    fun `validateAndLogin fails with empty or malformed fields`() {
        viewModel.onLoginEmailChanged("bad-email")
        viewModel.onLoginPasswordChanged("123")

        viewModel.validateAndLogin(onSuccess = {})

        val state = viewModel.loginState.value
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertNull(viewModel.currentUser.value)
    }

    @Test
    fun `logout clears user session and returns to welcome mode`() {
        viewModel.logout()
        assertNull(viewModel.currentUser.value)
        assertEquals(AuthMode.WELCOME, viewModel.authMode.value)
    }
}
