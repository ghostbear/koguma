/*
 * Copyright (C) 2022 ghostbear
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package me.ghostbear.koguma.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.ghostbear.koguma.R
import me.ghostbear.koguma.model.Status
import me.ghostbear.koguma.ui.main.MainViewModel.Event
import me.ghostbear.koguma.util.toast

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onSave = f@{ uri: Uri ->
        scope.launch {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                 viewModel.save(outputStream)
            }
        }
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.openUri = it
    }
    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let(onSave)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.app_name))
                },
                actions = {
                    IconButton(onClick = { openDocumentLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Outlined.FileOpen, contentDescription = "open_file")
                    }
                    IconButton(
                        onClick = {
                            if (viewModel.openUri != null) {
                                onSave(viewModel.openUri!!)
                            } else {
                                createDocumentLauncher.launch("details.json")
                            }
                        },
                        enabled = viewModel.isSavable
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = "save_file")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = viewModel.title.orEmpty(),
                onValueChange = { viewModel.title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Title") }
            )
            OutlinedTextField(
                value = viewModel.author.orEmpty(),
                onValueChange = { viewModel.author = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Author") }
            )
            OutlinedTextField(
                value = viewModel.artist.orEmpty(),
                onValueChange = { viewModel.artist = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Artist") }
            )
            OutlinedTextField(
                value = viewModel.description.orEmpty(),
                onValueChange = { viewModel.description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Description") }
            )
            OutlinedTextField(
                value = viewModel.genre.orEmpty(),
                onValueChange = { viewModel.genre = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Genre") },
                visualTransformation = {
                    val transformed = buildAnnotatedString {
                        val strings = it.split(",\\s*".toRegex())
                        strings.forEachIndexed { index, string ->
                            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append(string)
                            }
                            if (index != strings.lastIndex) append(", ")
                        }
                    }
                    val offsetMapping =  object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int = transformed.length

                        override fun transformedToOriginal(offset: Int): Int = it.length
                    }
                    TransformedText(
                        transformed,
                        offsetMapping
                    )
                }
            )
            Text(
                text = buildAnnotatedString {
                    append("Use a comma (")
                    withStyle(style = SpanStyle(background = Color.LightGray, letterSpacing = 8.sp)) {
                        append(",")
                    }
                    append(") to separate the genres")
                },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Status")
            Status.values.forEach {
                val onClick = { viewModel.status = it }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = viewModel.status == it, onClick = onClick)
                    Text(text = it.name)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is Event.FailureReadingFile -> context.toast("Failure reading from file: ${event.error.message}")
                is Event.FailureWritingFile -> context.toast("Failure writing from file: ${event.error.message}")
                Event.SuccessReadingFile -> context.toast("Success reading file")
                Event.SuccessWritingFile -> context.toast("Success writing file")
            }
        }
    }

    LaunchedEffect(viewModel.openUri) {
        val uri = viewModel.openUri ?: return@LaunchedEffect
        context.contentResolver.openInputStream(uri)!!.use { inputStream ->
            viewModel.load(inputStream)
        }
    }
}