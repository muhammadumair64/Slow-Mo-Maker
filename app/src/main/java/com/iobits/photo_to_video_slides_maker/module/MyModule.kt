package com.iobits.photo_to_video_slides_maker.module

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.net.ssl.*

@Module
@InstallIn(SingletonComponent::class)
class MyModule {

    var context: Context? = null
    @Singleton
    @Provides
    fun provideContext(application: Application): Context {
        context = application.applicationContext

        return context!!
    }
    /** new text */
}
