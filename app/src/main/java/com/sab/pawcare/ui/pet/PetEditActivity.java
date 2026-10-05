package com.sab.pawcare.ui.pet;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.sab.pawcare.R;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.data.entity.PetPhoto;
import com.sab.pawcare.databinding.ActivityPetEditBinding;
import com.sab.pawcare.session.SessionManager;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.PhotoStore;
import com.sab.pawcare.util.Validation;

import java.io.File;

public class PetEditActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "pet_id";
    private ActivityPetEditBinding binding;
    private long petId = -1;
    private Pet existing;
    private String photoPath;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                String copied = PhotoStore.copyIntoApp(this, uri);
                if (copied == null) {
                    Snackbar.make(binding.getRoot(), "Could not save that photo", Snackbar.LENGTH_LONG).show();
                    return;
                }
                photoPath = copied;
                binding.petPhoto.setImageURI(Uri.fromFile(new File(copied)));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPetEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        String[] types = getResources().getStringArray(R.array.animal_types);
        binding.inputType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types));
        petId = getIntent().getLongExtra(EXTRA_PET_ID, -1);
        binding.btnPhoto.setOnClickListener(v -> pickImage.launch("image/*"));
        binding.btnSave.setOnClickListener(v -> save());
        if (petId > 0) {
            binding.toolbar.setTitle("Edit pet");
            AppExecutors.IO.execute(() -> {
                existing = AppDatabase.get(this).petDao().findById(petId);
                runOnUiThread(this::bindExisting);
            });
        }
    }

    private void bindExisting() {
        if (existing == null) return;
        binding.inputName.setText(existing.name);
        binding.inputType.setText(existing.animalType, false);
        binding.inputBreed.setText(existing.breed);
        binding.inputAge.setText(String.valueOf(existing.ageYears));
        binding.inputWeight.setText(String.valueOf(existing.weightKg));
        binding.inputAllergies.setText(existing.allergies);
        binding.inputDiet.setText(existing.dietaryPreferences);
        binding.inputToys.setText(existing.favouriteToys);
        binding.inputMedical.setText(existing.medicalNotes);
        binding.inputVaccines.setText(existing.vaccinationHistory);
        photoPath = existing.photoUri;
        if (photoPath != null && !photoPath.isEmpty()) {
            File f = new File(photoPath);
            if (f.exists()) binding.petPhoto.setImageURI(Uri.fromFile(f));
        }
    }

    private void save() {
        String name = String.valueOf(binding.inputName.getText()).trim();
        String type = String.valueOf(binding.inputType.getText()).trim();
        String breed = String.valueOf(binding.inputBreed.getText()).trim();
        String ageS = String.valueOf(binding.inputAge.getText()).trim();
        String weightS = String.valueOf(binding.inputWeight.getText()).trim();
        binding.layoutName.setError(null);
        binding.layoutType.setError(null);
        if (Validation.isBlank(name)) {
            binding.layoutName.setError("Name is required");
            return;
        }
        if (Validation.isBlank(type)) {
            binding.layoutType.setError("Animal type is required");
            return;
        }
        int age = 0;
        double weight = 0;
        try {
            if (!ageS.isEmpty()) age = Integer.parseInt(ageS);
        } catch (NumberFormatException e) {
            binding.layoutAge.setError("Enter a whole number");
            return;
        }
        try {
            if (!weightS.isEmpty()) weight = Double.parseDouble(weightS);
        } catch (NumberFormatException e) {
            binding.layoutWeight.setError("Enter a number");
            return;
        }
        int finalAge = age;
        double finalWeight = weight;
        AppExecutors.IO.execute(() -> {
            AppDatabase db = AppDatabase.get(this);
            Pet pet = existing != null ? existing : new Pet();
            pet.ownerId = new SessionManager(this).getUserId();
            pet.name = name;
            pet.animalType = type;
            pet.breed = breed;
            pet.ageYears = finalAge;
            pet.weightKg = finalWeight;
            pet.allergies = String.valueOf(binding.inputAllergies.getText()).trim();
            pet.dietaryPreferences = String.valueOf(binding.inputDiet.getText()).trim();
            pet.favouriteToys = String.valueOf(binding.inputToys.getText()).trim();
            pet.medicalNotes = String.valueOf(binding.inputMedical.getText()).trim();
            pet.vaccinationHistory = String.valueOf(binding.inputVaccines.getText()).trim();
            pet.photoUri = photoPath;
            if (existing == null) {
                pet.createdAt = System.currentTimeMillis();
                long id = db.petDao().insert(pet);
                if (photoPath != null) {
                    PetPhoto extra = new PetPhoto();
                    extra.petId = id;
                    extra.uri = photoPath;
                    extra.createdAt = System.currentTimeMillis();
                    db.petPhotoDao().insert(extra);
                }
            } else {
                db.petDao().update(pet);
            }
            runOnUiThread(() -> {
                Snackbar.make(binding.getRoot(), "Pet saved", Snackbar.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
