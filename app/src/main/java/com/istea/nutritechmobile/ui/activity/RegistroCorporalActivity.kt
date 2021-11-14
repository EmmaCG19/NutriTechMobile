package com.istea.nutritechmobile.ui.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.istea.nutritechmobile.R
import com.istea.nutritechmobile.data.RegistroCorporal
import com.istea.nutritechmobile.data.UserResponse
import com.istea.nutritechmobile.firebase.FirebaseAuthManager
import com.istea.nutritechmobile.firebase.FirebaseFirestoreManager
import com.istea.nutritechmobile.firebase.FirebaseStorageManager
import com.istea.nutritechmobile.helpers.CameraManager
import com.istea.nutritechmobile.helpers.NOTIMPLEMENTEDYET
import com.istea.nutritechmobile.helpers.UIManager
import com.istea.nutritechmobile.model.RegistroCorporalRepositoryImp
import com.istea.nutritechmobile.presenter.RegistroCorporalPresenterImp
import com.istea.nutritechmobile.presenter.interfaces.IRegistroCorporalPresenter
import com.istea.nutritechmobile.ui.interfaces.IRegistroCorporalView
import kotlinx.coroutines.*

class RegistroCorporalActivity : AppCompatActivity(), IRegistroCorporalView {
    private lateinit var toolbar: Toolbar
    private lateinit var etPeso: EditText
    private lateinit var etCintura: EditText
    private lateinit var imgEstadoFisico: ImageView
    private lateinit var btnTakePhoto: ImageButton
    private lateinit var btnDeletePhoto: ImageButton
    private lateinit var btnGuardar: Button
    private lateinit var hiddenFileUpload: TextView
    private lateinit var hiddenImageName: TextView
    private lateinit var txtPhotoAddThumbnail: TextView
    private lateinit var imgPhotoAddThumbnail: ImageView
    private lateinit var bottomNavigationView: BottomNavigationView
    private var pacienteLogueado: UserResponse? = null

    private val firebaseStorageManager: FirebaseStorageManager by lazy {
        FirebaseStorageManager(this, FirebaseAuthManager())
    }

    private val camera: CameraManager by lazy {
        CameraManager(this, imgEstadoFisico, hiddenFileUpload, hiddenImageName)
    }

    private val presenter: IRegistroCorporalPresenter by lazy {
        RegistroCorporalPresenterImp(
            this,
            RegistroCorporalRepositoryImp(FirebaseFirestoreManager(this)),
            firebaseStorageManager,
            camera
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_corporal)
        setupUI()
    }

    private fun isFirstLogin(): Boolean {
        return intent?.extras?.getBoolean(FIRST_LOGIN) ?: false
    }

    private fun patientDataFromProfile(): UserResponse? {
        return intent?.extras?.getParcelable(USER_TO_UPDATE)
    }

    private fun setupUI() {
        etPeso = findViewById(R.id.etPeso)
        etCintura = findViewById(R.id.etCintura)
        imgEstadoFisico = findViewById(R.id.imgEstadoFisico)
        txtPhotoAddThumbnail = findViewById(R.id.txtPhotoAddThumbnail)
        imgPhotoAddThumbnail = findViewById(R.id.imgPhotoAddThumbnail)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnDeletePhoto = findViewById(R.id.btnDeletePhoto)
        btnGuardar = findViewById(R.id.btnGuardar)
        hiddenFileUpload = findViewById(R.id.hiddenFileUpload)
        hiddenImageName = findViewById(R.id.hiddenImageName)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        btnGuardar.isEnabled = false

        setupToolbar()
        setupBottomNavigationBar(bottomNavigationView)
        enableDefaultPhotoThumbnail()
        validateForm()
        bindEvents()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //Hiding default app icon
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun bindEvents() {

        btnTakePhoto.setOnClickListener {
            camera.takePhoto()
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1000)
                disableDefaultPhotoThumbnail()
            }

        }

        btnDeletePhoto.setOnClickListener {
            camera.cleanPhoto()
            hiddenFileUpload.text = ""
            enableDefaultPhotoThumbnail()
        }

        btnGuardar.setOnClickListener {
            submitForm()
        }

        hiddenFileUpload.addTextChangedListener {
            validateForm()
        }

        etPeso.addTextChangedListener {
            validateForm()
        }

        etCintura.addTextChangedListener {
            validateForm()
        }

    }

    private fun buildCorporalRegistry(): RegistroCorporal {
        return RegistroCorporal(
            hiddenImageName.text.toString(),
            hiddenFileUpload.text.toString(),
            etPeso.text.toString().toFloatOrNull() ?: 0f,
            etCintura.text.toString().toFloatOrNull() ?: 0f,
        )
    }

    private fun buildPacienteFromForm(): UserResponse? {

        if (isFirstLogin()) {
            pacienteLogueado = patientDataFromProfile()
        }

        pacienteLogueado?.let { it ->
            it.Peso = etPeso.text.toString().toFloatOrNull() ?: 0f
            it.MedidaCintura = etCintura.text.toString().toFloatOrNull() ?: 0f
            return it
        }
        return null;
    }


    private fun submitForm() {
        val registro = buildCorporalRegistry()
        val loggedUserMail = FirebaseAuthManager().getAuthEmail()
        val paciente = buildPacienteFromForm()

        lifecycleScope.launch(Dispatchers.Main) {
            presenter.addCorporalRegistry(loggedUserMail, registro)
            presenter.updatePaciente(paciente, isFirstLogin())
        }
    }

    private suspend fun getLoggedUser(): UserResponse? {
        return withContext(Dispatchers.IO) {
            presenter.getLoggedUser()
        }
    }


    private fun enableDefaultPhotoThumbnail() {
        imgEstadoFisico.isVisible = false
        imgPhotoAddThumbnail.isVisible = true
        txtPhotoAddThumbnail.isVisible = true
    }

    private fun disableDefaultPhotoThumbnail() {
        imgEstadoFisico.isVisible = true
        imgPhotoAddThumbnail.isVisible = false
        txtPhotoAddThumbnail.isVisible = false
    }

    private fun validateForm() {
        deactivateSubmitButton()

        if (etCintura.text.isNotEmpty() && etPeso.text.isNotEmpty()) {
            if (hiddenFileUpload.text.isNotEmpty()) {
                activateSubmitButton()
            }
        }

    }

    private fun activateSubmitButton() {
        btnGuardar.isEnabled = true
    }

    private fun deactivateSubmitButton() {
        btnGuardar.isEnabled = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        camera.activityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camera.requestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        lifecycleScope.launch(Dispatchers.Main) {
            pacienteLogueado = getLoggedUser()
        }
        super.onResume()
    }

    override fun resetForm() {
        finish()
        overridePendingTransition(0, 0)
        Intent(this@RegistroCorporalActivity, this::class.java).apply {
            startActivity(this)
        }
        overridePendingTransition(0, 0)
    }

    override fun setupBottomNavigationBar(bottomNavigationView: BottomNavigationView) {
        val mOnNavigationItemSelectedListener =
            BottomNavigationView.OnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.registro_diario -> {
                        goToDailyRegistryView()
                        return@OnNavigationItemSelectedListener true
                    }
                    R.id.recetas -> {
                        goToRecipesView()
                        return@OnNavigationItemSelectedListener true

                    }
                    R.id.progreso -> {
                        goToProgressView()
                        return@OnNavigationItemSelectedListener true

                    }
                    R.id.info_personal -> {
                        goToProfileView()
                        return@OnNavigationItemSelectedListener true

                    }
                }
                false
            }
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    override fun goToDailyRegistryView() {
        Intent(this@RegistroCorporalActivity, DailyRegistryActivity::class.java).apply {
            startActivity(this)
        }
    }

    override fun goToRecipesView() {
        showInProgressMessage()
    }

    override fun goToProgressView() {
        resetForm()
    }

    override fun goToProfileView() {
        Intent(this@RegistroCorporalActivity, PerfilPacienteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(this)
            finish()
        }
    }

    override fun goToMainScreenView() {
        Intent(this@RegistroCorporalActivity, PaginaPrincipalActivity::class.java).apply {
            startActivity(this)
            finish()
        }
    }

    override fun goBackToLogin() {
        Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(this)
            finish()
        }
    }

    override fun showInProgressMessage() {
        UIManager.showMessageShort(this, NOTIMPLEMENTEDYET)
    }
}