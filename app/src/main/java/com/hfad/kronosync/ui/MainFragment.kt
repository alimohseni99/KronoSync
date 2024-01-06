package com.hfad.kronosync.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.hfad.kronosync.R
import com.hfad.kronosync.viewmodel.SharedViewModel

class MainFragment : Fragment() {

    private lateinit var favoriteImageView: ImageView
    private lateinit var deleteButton: ImageView
    private lateinit var editTextUserInput: EditText
    private lateinit var favoritesSpinner: Spinner
    private val favoritesList = mutableListOf<String>()
    private var selectedFavorite: String? = null

    private val sharedViewModel: SharedViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        favoriteImageView = view.findViewById(R.id.favoriteImageView)
        deleteButton = view.findViewById(R.id.deleteSelectedFavoriteButton)
        editTextUserInput = view.findViewById(R.id.editTextUserInput)
        favoritesSpinner = view.findViewById(R.id.favoritesSpinner)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = activity?.getSharedPreferences("MyPref", Context.MODE_PRIVATE)
        loadFavorites(sharedPref)

        val spinnerAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            favoritesList
        ) {
            // färgen på alla items inuti spinner, just nu är det svart kanske ändrar senare
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.setTextColor(Color.BLACK)
                return textView
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val textView = super.getDropDownView(position, convertView, parent) as TextView
                textView.setTextColor(Color.BLACK)
                return textView
            }
        }
        favoritesSpinner.adapter = spinnerAdapter
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        favoritesSpinner.adapter = spinnerAdapter

        favoriteImageView.setOnClickListener {
            val programCode = editTextUserInput.text.toString().trim()
            if (programCode.isNotBlank()) {
                addFavorite(programCode, sharedPref, spinnerAdapter)
                // Sätt programkoden i SharedViewModel
                sharedViewModel.selectedProgramCode.value = programCode

            } else {
                Toast.makeText(
                    context,
                    "Ange en programkod först",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        favoritesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedFavorite = parent.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedFavorite = null
            }
        }

        deleteButton.setOnClickListener {
            selectedFavorite?.let {
                removeSelectedFavorite(it, sharedPref, spinnerAdapter)
            }
        }
        fun navigateToScheduleFragment(view: View) {
            view.findNavController().navigate(R.id.action_mainFragment_to_scheduleFragment)
        }

        val viewScheduleButton = view.findViewById<Button>(R.id.showSchedule)
        viewScheduleButton.setOnClickListener {
            val programCode = editTextUserInput.text.toString().trim()
            val selectedFavorite = favoritesSpinner.selectedItem as String?
            val pattern = Regex("^[A-Z]{5}\\.\\d{5}\\.\\d{2}$")

            when {
                programCode.isNotBlank() && pattern.matches(programCode) -> {
                    sharedViewModel.selectedProgramCode.value = programCode
                    navigateToScheduleFragment(view)
                }

                selectedFavorite != null && pattern.matches(selectedFavorite) -> {
                    sharedViewModel.selectedProgramCode.value = selectedFavorite
                    navigateToScheduleFragment(view)
                }

                programCode.isNotBlank() && !pattern.matches(programCode) -> {
                    Toast.makeText(
                        context,
                        "Ogiltigt format. Använd formatet ABCDE.12345.67",
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> {
                    Toast.makeText(
                        context,
                        "Ange en programkod eller välj en från favorit listan först",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    private fun removeSelectedFavorite(
        itemToRemove: String,
        sharedPref: SharedPreferences?,
        spinnerAdapter: ArrayAdapter<String>
    ) {
        if (favoritesList.contains(itemToRemove)) {
            favoritesList.remove(itemToRemove)
            spinnerAdapter.notifyDataSetChanged()

            sharedPref?.edit()?.let { editor ->
                val favoritesSet =
                    sharedPref.getStringSet("favorites", mutableSetOf())?.toMutableSet()
                val wasRemoved = favoritesSet?.remove(itemToRemove)
                editor.putStringSet("favorites", favoritesSet)

                if (wasRemoved == true) {
                    if (editor.commit()) {
                        Toast.makeText(
                            context,
                            "$itemToRemove borttagen från favoriter",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Kunde inte ta bort $itemToRemove",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "$itemToRemove fanns inte i favoriter",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun loadFavorites(sharedPref: SharedPreferences?) {
        sharedPref?.getStringSet("favorites", null)?.let { savedFavorites ->
            favoritesList.clear()
            favoritesList.addAll(savedFavorites)
            favoritesList.sort()
            val adapter = favoritesSpinner.adapter as? ArrayAdapter<*>
            adapter?.notifyDataSetChanged()
        }
    }


    private fun addFavorite(
        programCode: String,
        sharedPref: SharedPreferences?,
        spinnerAdapter: ArrayAdapter<String>
    ) {
        val pattern = Regex("^[A-Z]{5}\\.\\d{5}\\.\\d{2}$")

        // kontrollerar om programkoden matchar regex-mönstret
        if (pattern.matches(programCode)) {
            // om programkoden är giltig och inte redan finns i listan, lägger till den i lkistan
            if (!favoritesList.contains(programCode)) {
                favoritesList.add(programCode)
                spinnerAdapter.notifyDataSetChanged()
                sharedPref?.edit()?.apply {
                    putStringSet("favorites", favoritesList.toSet())
                    apply()
                }

                Toast.makeText(context, "Programkod sparad som favorit", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    "Programkod är redan sparad som favorit",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // om programkoden inte matchar mönstret, visas det ett felmeddelanded nedan
            Toast.makeText(
                context,
                "Ogiltigt format. Använd formatet ABCDE.12345.67",
                Toast.LENGTH_LONG
            ).show()
        }
    }

}
