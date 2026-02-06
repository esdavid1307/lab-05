package com.example.lab5_starter;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;
    private Button deleteCityButton;
    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    // Firebase
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);
        deleteCityButton = findViewById(R.id.buttonDeleteCity);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // firebase setup
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        loadCitiesFromFirebase();

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });

        deleteCityButton.setOnClickListener(view -> {
            toDeleteCity();
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        String oldName = city.getName();
        city.setName(title);
        city.setProvince(year);
        // update firebase delete old, add new
        citiesRef.document(oldName).delete()
                .addOnSuccessListener(unused -> {
                    citiesRef.document(city.getName()).set(city);
                });
    }
    @Override
    public void addCity(City city){
        citiesRef.document(city.getName()).set(city);
    }
    private void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");

        citiesRef.document(m1.getName()).set(m1);
        citiesRef.document(m2.getName()).set(m2);
    }

    private void loadCitiesFromFirebase(){
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }
            if (value != null) {
                cityArrayList.clear();

                // if emppty add some cities
                if (value.isEmpty()) {
                    addDummyData();
                } else {
                    // load from Firebase
                    for (QueryDocumentSnapshot doc : value) {
                        String name = doc.getString("name");
                        String province = doc.getString("province");
                        cityArrayList.add(new City(name, province));
                    }
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });
    }
    private void toDeleteCity() {
        EditText cityInput = new EditText(this);
        cityInput.setHint("Enter city name");

        new AlertDialog.Builder(this)
                .setTitle("Delete City")
                .setView(cityInput)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String cityName = cityInput.getText().toString().trim();
                    if (cityName.isEmpty()) {
                        Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    deleteCity(cityName);
                }).setNegativeButton("Cancel", null).show();
    }
    private void deleteCity(String cityName) {
        citiesRef.document(cityName).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        citiesRef.document(cityName).delete();
                        Toast.makeText(this, cityName + " deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}