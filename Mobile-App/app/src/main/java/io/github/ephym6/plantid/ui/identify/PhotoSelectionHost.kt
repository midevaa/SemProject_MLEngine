package io.github.ephym6.plantid.ui.identify

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.data.image.PhotoError
import io.github.ephym6.plantid.ui.components.LoadingIndicator
import io.github.ephym6.plantid.ui.theme.Spacing

/**
 * Everything around choosing a photo that is not a screen: the source sheet,
 * the system photo picker and camera launchers, the "preparing" dialog and
 * error dialogs. Place once, at the app level.
 *
 * No runtime permissions are needed: the photo picker grants access to the
 * chosen photo only, and the system camera app writes into a file we share
 * with it through a FileProvider.
 */
@Composable
fun PhotoSelectionHost(
    viewModel: PhotoSelectionViewModel,
    onPhotoReady: (photoId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cameraAvailable = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onPhotoPicked(uri?.toString()) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved -> viewModel.onCaptureResult(saved) }

    val launchPicker = {
        viewModel.onPickerLaunched()
        try {
            pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: ActivityNotFoundException) {
            viewModel.onPickerUnavailable()
        }
    }

    LaunchedEffect(state.captureRequest) {
        val target = state.captureRequest ?: return@LaunchedEffect
        viewModel.onCaptureLaunched()
        try {
            cameraLauncher.launch(target.uri.toUri())
        } catch (_: ActivityNotFoundException) {
            viewModel.onCameraUnavailable()
        }
    }

    LaunchedEffect(state.readyPhotoId) {
        val photoId = state.readyPhotoId ?: return@LaunchedEffect
        onPhotoReady(photoId)
        viewModel.onReadyPhotoHandled()
    }

    if (state.isSourceChooserVisible) {
        PhotoSourceSheet(
            cameraAvailable = cameraAvailable,
            onTakePhoto = viewModel::requestCapture,
            onChooseFromPhotos = launchPicker,
            onDismiss = viewModel::dismissSourceChooser,
        )
    }

    if (state.isPreparing) {
        PreparingPhotoDialog()
    }

    state.error?.let { error ->
        PhotoErrorDialog(
            error = error,
            onDismiss = viewModel::dismissError,
            onChooseFromPhotos = if (error == PhotoError.CameraUnavailable) {
                {
                    viewModel.dismissError()
                    launchPicker()
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun PreparingPhotoDialog() {
    // Not dismissible: the copy is short and cancelling halfway would leave a partial file.
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            LoadingIndicator(
                message = stringResource(R.string.photo_preparing),
                modifier = Modifier.padding(Spacing.sm),
            )
        }
    }
}

@Composable
private fun PhotoErrorDialog(
    error: PhotoError,
    onDismiss: () -> Unit,
    onChooseFromPhotos: (() -> Unit)?,
) {
    val (titleRes, messageRes) = when (error) {
        PhotoError.CouldNotOpen -> R.string.photo_error_open_title to R.string.photo_error_open_message
        PhotoError.CameraUnavailable -> R.string.photo_error_camera_title to R.string.photo_error_camera_message
        PhotoError.PickerUnavailable -> R.string.photo_error_picker_title to R.string.photo_error_picker_message
        PhotoError.CouldNotPrepare -> R.string.photo_error_prepare_title to R.string.photo_error_prepare_message
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_error), contentDescription = null) },
        title = { Text(stringResource(titleRes)) },
        text = { Text(stringResource(messageRes)) },
        confirmButton = {
            if (onChooseFromPhotos != null) {
                TextButton(onClick = onChooseFromPhotos) { Text(stringResource(R.string.photo_source_gallery)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
            }
        },
        dismissButton = if (onChooseFromPhotos != null) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } }
        } else {
            null
        },
    )
}
