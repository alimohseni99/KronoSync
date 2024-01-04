package com.hfad.kronosync.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val selectedProgramCode = MutableLiveData<String>()
}
