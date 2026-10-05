package com.sab.pawcare.ui.pet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.sab.pawcare.data.AppDatabase;
import com.sab.pawcare.data.entity.Pet;
import com.sab.pawcare.data.entity.PetPhoto;
import com.sab.pawcare.databinding.ActivityPetDetailBinding;
import com.sab.pawcare.ui.expense.ExpensesActivity;
import com.sab.pawcare.ui.routine.RoutineEditActivity;
import com.sab.pawcare.util.AppExecutors;
import com.sab.pawcare.util.PhotoStore;

import java.io.File;
import java.util.List;

public class PetDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PET_ID = "pet_id";
    private ActivityPetDetailBinding binding;
    private long petId;
    private Pet pet;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                String copied = PhotoStore.copyIntoApp(this, uri);
                if (copied == null) {
                    Snackbar.make(binding.getRoot(), "Could not save that photo", Snackbar.LENGTH_LONG).show();
                    return;
                }
                AppExecutors.IO.execute(() -> {
                    PetPhoto photo = new PetPhoto();
                    photo.petId = petId;
                    photo.uri = copied;
                    photo.createdAt = System.currentTimeMillis();
                    AppDatabase.get(this).petPhotoDao().insert(photo);
                    if (pet != null && (pet.photoUri == null || pet.photoUri.isEmpty())) {
                        pet.photoUri = copied;
                        AppDatabase.get(this).petDao().update(pet);
                    }
                    runOnUiThread(() -> Snackbar.make(binding.getRoot(), "Photo added", Snackbar.LENGTH_SHORT).show());
                });
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPetDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        petId = getIntent().getLongExtra(EXTRA_PET_ID, -1);
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, PetEditActivity.class);
            i.putExtra(PetEditActivity.EXTRA_PET_ID, petId);
            startActivity(i);
        });
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.btnAddPhoto.setOnClickListener(v -> pickImage.launch("image/*"));
        binding.btnAddRoutine.setOnClickListener(v -> {
            Intent i = new Intent(this, RoutineEditActivity.class);
            i.putExtra(RoutineEditActivity.EXTRA_PET_ID, petId);
            startActivity(i);
        });
        binding.btnExpenses.setOnClickListener(v -> {
            Intent i = new Intent(this, ExpensesActivity.class);
            i.putExtra(ExpensesActivity.EXTRA_PET_ID, petId);
            startActivity(i);
        });
        AppDatabase.get(this).petDao().observeById(petId).observe(this, this::bindPet);
        AppDatabase.get(this).petPhotoDao().observeByPet(petId).observe(this, this::bindPhotos);
    }

    private void bindPet(Pet p) {
        pet = p;
        if (p == null) {
            finish();
            return;
        }
        binding.toolbar.setTitle(p.name);
        binding.petName.setText(p.name);
        binding.petMeta.setText(p.animalType + " • " + p.breed);
        binding.facts.setText("Age: " + p.ageYears + " years\nWeight: " + p.weightKg + " kg");
        binding.allergies.setText(empty(p.allergies));
        binding.diet.setText(empty(p.dietaryPreferences));
        binding.toys.setText(empty(p.favouriteToys));
        binding.medical.setText(empty(p.medicalNotes));
        binding.vaccines.setText(empty(p.vaccinationHistory));
        if (p.photoUri != null && !p.photoUri.isEmpty()) {
            File f = new File(p.photoUri);
            if (f.exists()) binding.heroPhoto.setImageURI(Uri.fromFile(f));
        }
    }

    private String empty(String s) {
        return s == null || s.trim().isEmpty() ? "Not listed" : s;
    }

    private void bindPhotos(List<PetPhoto> photos) {
        LinearLayout strip = binding.photoStrip;
        strip.removeAllViews();
        if (photos == null) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (PetPhoto photo : photos) {
            ImageView iv = (ImageView) inflater.inflate(com.sab.pawcare.R.layout.item_photo_thumb, strip, false);
            File f = new File(photo.uri);
            if (f.exists()) iv.setImageURI(Uri.fromFile(f));
            iv.setOnLongClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Remove photo?")
                        .setPositiveButton("Remove", (d, w) -> AppExecutors.IO.execute(() ->
                                AppDatabase.get(this).petPhotoDao().delete(photo)))
                        .setNegativeButton("Cancel", (d, w) -> { })
                        .show();
                return true;
            });
            strip.addView(iv);
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + (pet != null ? pet.name : "this pet") + "?")
                .setMessage("Routines, tasks, photos, and expenses for this pet will also be removed.")
                .setNegativeButton("Cancel", (d, w) -> { })
                .setPositiveButton("Delete", (d, w) -> AppExecutors.IO.execute(() -> {
                    if (pet != null) AppDatabase.get(this).petDao().delete(pet);
                    runOnUiThread(this::finish);
                }))
                .show();
    }
}
