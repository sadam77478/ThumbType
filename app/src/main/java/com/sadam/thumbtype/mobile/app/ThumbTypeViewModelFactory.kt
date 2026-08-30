package com.sadam.thumbtype.mobile.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.sadam.thumbtype.mobile.ThumbTypeApplication

class ThumbTypeViewModelFactory(
    private val application: ThumbTypeApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(ThumbTypeViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ThumbTypeViewModel(
            application = application,
            savedStateHandle = extras.createSavedStateHandle()
        ) as T
    }
}
