package com.youxel.library.utils

import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeTransition
import com.youxel.library.R

@BindingAdapter(value = ["imageUri", "successCallback"], requireAll = false)
fun ImageView.loadImage(imgUrl: Uri?, onLoadSuccess: (resource: Drawable) -> Unit = {}) {

    val requestOption = RequestOptions()
        .placeholder(R.drawable.ic_video)
        .error(R.drawable.ic_video)

    Glide.with(this.context)
        .load(imgUrl)
        .transition(DrawableTransitionOptions.withCrossFade())
        .apply(requestOption)
        .centerInside()
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .listener(GlideImageRequestListener(object :
            GlideImageRequestListener.Callback {
            override fun onFailure(message: String?) {
                Log.d("loadImage", "onFailure:-> $message")
            }

            override fun onSuccess(dataSource: String, resource: Drawable) {
                Log.d("loadImage", "onSuccess:-> load from $dataSource")
                onLoadSuccess(resource)
            }

        }))
        .into(this)
}


class GlideImageRequestListener(private val callback: Callback? = null) :
    RequestListener<Drawable> {

    interface Callback {
        fun onFailure(message: String?)
        fun onSuccess(dataSource: String, resource: Drawable)
    }

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        isFirstResource: Boolean
    ): Boolean {
        callback?.onFailure(e?.message)
        return false
    }

    override fun onResourceReady(
        resource: Drawable?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean {

        resource?.let {
            target?.onResourceReady(
                it,
                DrawableCrossFadeTransition(1000, isFirstResource)
            )
        }
        callback?.onSuccess(dataSource.toString(), resource!!)
        return true
    }
}
