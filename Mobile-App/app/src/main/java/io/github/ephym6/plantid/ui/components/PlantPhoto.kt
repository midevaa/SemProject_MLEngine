package io.github.ephym6.plantid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.github.ephym6.plantid.R
import io.github.ephym6.plantid.ui.theme.Sizes
import io.github.ephym6.plantid.ui.theme.Spacing
import java.io.File

enum class PhotoLoadState { Loading, Loaded, Unavailable }

/**
 * The user's plant photo, scaled to fit (never cropped or stretched) inside a
 * rounded frame. Coil decodes it off the main thread at display size, not full
 * resolution, and applies the camera's EXIF rotation.
 *
 * @param photo null, a missing file or an undecodable file all show [unavailableMessage].
 */
@Composable
fun PlantPhoto(
    photo: File?,
    contentDescription: String,
    unavailableMessage: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 4f / 5f,
    onLoadStateChange: (PhotoLoadState) -> Unit = {},
) {
    var loadState by remember(photo) {
        mutableStateOf(if (photo == null) PhotoLoadState.Unavailable else PhotoLoadState.Loading)
    }
    val currentOnLoadStateChange by rememberUpdatedState(onLoadStateChange)
    LaunchedEffect(loadState) { currentOnLoadStateChange(loadState) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                onState = { state ->
                    loadState = when (state) {
                        is AsyncImagePainter.State.Success -> PhotoLoadState.Loaded
                        is AsyncImagePainter.State.Error -> PhotoLoadState.Unavailable
                        else -> PhotoLoadState.Loading
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        when (loadState) {
            PhotoLoadState.Loading -> CircularProgressIndicator()
            PhotoLoadState.Unavailable -> PhotoUnavailable(unavailableMessage)
            PhotoLoadState.Loaded -> Unit
        }
    }
}

@Composable
private fun PhotoUnavailable(message: String) {
    Column(
        modifier = Modifier.padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_error),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Sizes.iconLarge),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
