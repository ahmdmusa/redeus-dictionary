package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ApiKeyDirectLinkBanner(
    title: String,
    subtitle: String,
    url: String,
    buttonText: String = "Get key",
    testTag: String = "api_key_direct_link_banner"
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Surface(
        onClick = { openExternalUrl(uriHandler, context, url) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun GeminiKeyRowItem(
    index: Int,
    apiKey: String,
    isActive: Boolean,
    onKeyChange: (String) -> Unit,
    onSelectActive: () -> Unit,
    onDelete: () -> Unit,
    labelPrefix: String = "Gemini API Key",
    testTagPrefix: String = "gemini_key",
    directLinkUrl: String? = null,
    directLinkLabel: String? = null
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    var textInput by remember(apiKey) { mutableStateOf(apiKey) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isActive) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectActive() }
                ) {
                    RadioButton(
                        selected = isActive,
                        onClick = onSelectActive,
                        modifier = Modifier.testTag("${testTagPrefix}_${index + 1}_radio")
                    )
                    Text(
                        text = "$labelPrefix ${index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isActive) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("delete_${testTagPrefix}_${index + 1}_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete $labelPrefix ${index + 1}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    onKeyChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_${index + 1}_input"),
                label = { Text("$labelPrefix ${index + 1}") },
                placeholder = { Text("Paste API key") },
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { isVisible = !isVisible },
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("toggle_${testTagPrefix}_${index + 1}_visibility")
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isVisible) "Hide $labelPrefix ${index + 1}" else "Show $labelPrefix ${index + 1}"
                        )
                    }
                },
                singleLine = true
            )

            if (!directLinkUrl.isNullOrBlank()) {
                TextButton(
                    onClick = { openExternalUrl(uriHandler, context, directLinkUrl) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("${testTagPrefix}_${index + 1}_direct_link")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(directLinkLabel ?: "Get API key")
                }
            }
        }
    }
}

@Composable
fun SingleApiKeyCard(
    heading: String,
    description: String,
    currentKey: String,
    placeholder: String,
    testTagPrefix: String,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    disabledHint: String = "No key set — this feature is disabled until you add one.",
    directLinkUrl: String? = null,
    directLinkLabel: String? = null,
    directLinkSubtitle: String? = null
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    var textInput by remember(currentKey) { mutableStateOf(currentKey) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!directLinkUrl.isNullOrBlank()) {
                ApiKeyDirectLinkBanner(
                    title = directLinkLabel ?: "Get API key",
                    subtitle = directLinkSubtitle ?: directLinkUrl.removePrefix("https://").removeSuffix("/"),
                    url = directLinkUrl,
                    testTag = "${testTagPrefix}_direct_link_banner"
                )
            }

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_input"),
                label = { Text(heading) },
                placeholder = { Text(placeholder) },
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { isVisible = !isVisible },
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("${testTagPrefix}_visibility_toggle")
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isVisible) "Hide API key" else "Show API key"
                        )
                    }
                },
                singleLine = true
            )

            if (!directLinkUrl.isNullOrBlank()) {
                TextButton(
                    onClick = { openExternalUrl(uriHandler, context, directLinkUrl) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("${testTagPrefix}_direct_link_under_input")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Get key directly: ${directLinkSubtitle ?: directLinkUrl}")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSaveKey(textInput.trim()) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("${testTagPrefix}_save_button")
                ) {
                    Text("Save key")
                }
                OutlinedButton(
                    onClick = {
                        textInput = ""
                        onClearKey()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("${testTagPrefix}_clear_button")
                ) {
                    Text("Clear")
                }
            }

            if (currentKey.isBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = disabledHint,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("${testTagPrefix}_disabled_hint")
                    )
                }
            }
        }
    }
}

internal fun openExternalUrl(
    uriHandler: androidx.compose.ui.platform.UriHandler,
    context: android.content.Context,
    url: String
) {
    try {
        uriHandler.openUri(url)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ApiKeyLink", "Error opening $url", e)
        }
    }
}
