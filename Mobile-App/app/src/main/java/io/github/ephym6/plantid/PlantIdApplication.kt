package io.github.ephym6.plantid

import android.app.Application
import io.github.ephym6.plantid.di.AppContainer

class PlantIdApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
