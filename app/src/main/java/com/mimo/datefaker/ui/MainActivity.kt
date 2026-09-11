package com.mimo.datefaker.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mimo.datefaker.R
import com.mimo.datefaker.data.AppListLoader
import com.mimo.datefaker.data.ConfigStore
import com.mimo.datefaker.data.ModuleConfig
import com.mimo.datefaker.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var config: ModuleConfig = ModuleConfig()
    private var draftFixedMillis: Long = ConfigStore.defaultFixedMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        config = ConfigStore.read(this)
        if (config.fixedMillis <= 0L) {
            draftFixedMillis = ConfigStore.defaultFixedMillis()
        } else {
            draftFixedMillis = config.fixedMillis
        }

        bindListeners()
        refreshUi()
        refreshStatus()
    }

    private fun bindListeners() {
        binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
            config = config.copy(enabled = checked)
            refreshPreview()
        }

        binding.cardTargetApp.setOnClickListener { showAppPicker() }

        binding.groupMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            config = config.copy(
                mode = if (checkedId == R.id.btnModeOffset) ModuleConfig.MODE_OFFSET else ModuleConfig.MODE_FIXED
            )
            refreshModePanels()
            refreshPreview()
        }

        binding.cardDate.setOnClickListener { pickDate() }
        binding.cardTime.setOnClickListener { pickTime() }

        binding.etOffsetDays.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                readOffsetDays()
                refreshPreview()
            }
        })

        binding.switchSpoofBuildTime.setOnCheckedChangeListener { _, checked ->
            config = config.copy(spoofBuildTime = checked)
        }

        binding.switchSpoofTimezone.setOnCheckedChangeListener { _, checked ->
            config = config.copy(spoofTimezone = checked)
            binding.tilTimezone.visibility = if (checked) View.VISIBLE else View.GONE
        }

        binding.etTimezone.setOnFocusChangeListener { _, has ->
            if (!has) {
                val id = binding.etTimezone.text?.toString()?.trim().orEmpty()
                if (id.isNotEmpty()) config = config.copy(timezoneId = id)
            }
        }

        binding.btnSave.setOnClickListener { save() }
    }

    private fun refreshUi() {
        binding.switchEnabled.isChecked = config.enabled
        binding.tvTargetApp.text = resolveTargetLabel()

        when (config.mode) {
            ModuleConfig.MODE_OFFSET -> binding.groupMode.check(R.id.btnModeOffset)
            else -> binding.groupMode.check(R.id.btnModeFixed)
        }

        binding.tvFakeDate.text = ConfigStore.formatDate(draftFixedMillis)
        binding.tvFakeTime.text = ConfigStore.formatTime(draftFixedMillis)
        binding.etOffsetDays.setText(config.offsetDays.toString())
        binding.switchSpoofBuildTime.isChecked = config.spoofBuildTime
        binding.switchSpoofTimezone.isChecked = config.spoofTimezone
        binding.etTimezone.setText(config.timezoneId)
        binding.tilTimezone.visibility = if (config.spoofTimezone) View.VISIBLE else View.GONE

        refreshModePanels()
        refreshPreview()
    }

    private fun resolveTargetLabel(): String {
        if (config.targetPackage.isBlank()) return getString(R.string.target_app_none)
        return try {
            val info = packageManager.getApplicationInfo(config.targetPackage, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Throwable) {
            config.targetPackage
        }
    }

    private fun refreshModePanels() {
        val fixed = config.mode == ModuleConfig.MODE_FIXED
        binding.panelFixed.visibility = if (fixed) View.VISIBLE else View.GONE
        binding.panelOffset.visibility = if (fixed) View.GONE else View.VISIBLE
    }

    private fun refreshPreview() {
        readOffsetDays()
        val draft = config.copy(fixedMillis = draftFixedMillis)
        val fake = ConfigStore.computePreview(draft)
        val real = System.currentTimeMillis()
        binding.tvPreviewFake.text = ConfigStore.formatDateTime(fake)
        binding.tvPreviewReal.text = ConfigStore.formatDateTime(real)
    }

    private fun refreshStatus() {
        // 模块自身进程不会被注入 XposedBridge，无法在此可靠探测激活状态。
        // 请以 LSPosed 管理器中的勾选状态为准。
        binding.tvModuleStatus.text = getString(R.string.status_check_lsposed)
    }

    private fun showAppPicker() {
        val apps = try {
            AppListLoader.load(packageManager)
        } catch (t: Throwable) {
            emptyList()
        }
        if (apps.isEmpty()) {
            Toast.makeText(this, R.string.app_list_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_app_picker, null)
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvApps)
        val search = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)

        val adapter = AppAdapter(apps) { app ->
            config = config.copy(targetPackage = app.packageName)
            binding.tvTargetApp.text = app.label
            dialogRef?.dismiss()
        }
        recycler.adapter = adapter
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                adapter.filter(s?.toString().orEmpty())
            }
        })

        dialogRef = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.target_app_title)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private var dialogRef: androidx.appcompat.app.AlertDialog? = null

    private fun pickDate() {
        val c = Calendar.getInstance().apply { timeInMillis = draftFixedMillis }
        DatePickerDialog(
            this,
            { _, y, m, d ->
                val next = Calendar.getInstance().apply {
                    timeInMillis = draftFixedMillis
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                draftFixedMillis = next.timeInMillis
                binding.tvFakeDate.text = ConfigStore.formatDate(draftFixedMillis)
                refreshPreview()
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickTime() {
        val c = Calendar.getInstance().apply { timeInMillis = draftFixedMillis }
        TimePickerDialog(
            this,
            { _, h, min ->
                val next = Calendar.getInstance().apply {
                    timeInMillis = draftFixedMillis
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                draftFixedMillis = next.timeInMillis
                binding.tvFakeTime.text = ConfigStore.formatTime(draftFixedMillis)
                refreshPreview()
            },
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun readOffsetDays() {
        val raw = binding.etOffsetDays.text?.toString()?.trim().orEmpty()
        val days = raw.toIntOrNull() ?: 0
        config = config.copy(offsetDays = days)
    }

    private fun save() {
        readOffsetDays()
        val tz = binding.etTimezone.text?.toString()?.trim().orEmpty()
        val toSave = config.copy(
            fixedMillis = draftFixedMillis,
            timezoneId = tz.ifBlank { "Asia/Shanghai" },
        )
        ConfigStore.write(this, toSave)
        config = toSave
        Toast.makeText(this, R.string.saved, Toast.LENGTH_LONG).show()
    }
}
