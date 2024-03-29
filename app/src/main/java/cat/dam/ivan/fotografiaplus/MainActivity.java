package cat.dam.ivan.fotografiaplus;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
{

    String currentPhotoPath;
    ImageView iv_imatge;
    Button btn_foto, btn_galeria, btn_rotar, btn_filtre, btn_save;
    Uri uriPhotoImage;
    ContentValues values;

    private final ActivityResultLauncher<Intent> activityResultLauncherGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                //here we will handle the result of our intent
                if (result.getResultCode() == Activity.RESULT_OK) {
                    //image picked
                    //get uri of image
                    Intent data = result.getData();
                    assert data != null;
                    Uri imageUri = data.getData();
                    System.out.println("galeria: "+imageUri);
                    iv_imatge.setImageURI(imageUri);
                } else {
                    //cancelled
                    Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                }
            }
    );
    private final ActivityResultLauncher<Intent> activityResultLauncherPhoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                //here we will handle the result of our intent
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
                    iv_imatge.setImageURI(uriPhotoImage); //Amb paràmetre EXIF podem canviar orientació (per defecte horiz en versions android antigues)
                    refreshGallery();//refresca gallery per veure nou fitxer
                        /* Intent data = result.getData(); //si volguessim només la miniatura
                        Uri imageUri = data.getData();
                        iv_imatge.setImageURI(imageUri);*/
                } else {
                    //cancelled
                    Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_imatge = findViewById(R.id.iv_foto);
        btn_foto = findViewById(R.id.btn_foto);
        btn_galeria = findViewById(R.id.btn_galeria);
        btn_rotar = findViewById(R.id.btn_rotar);
        btn_filtre = findViewById(R.id.btn_filtre);
        btn_save = findViewById(R.id.btn_save);

        btn_galeria.setOnClickListener(v -> {
            if (checkPermissions()) {
                openGallery();
            } else {
                askForPermissions();
            }
        });

        btn_foto.setOnClickListener(v -> {
            if (checkPermissions())
            {
                takePicture();
            } else {
                askForPermissions();
            }
        });

        btn_rotar.setOnClickListener(v -> {
            if(checkPermissions())
            {
                rotatePicture();
            } else {
                askForPermissions();
            }
        });

        btn_filtre.setOnClickListener(v -> {
            if(checkPermissions())
            {
                filterPicture();
            } else {
                askForPermissions();
            }
        });

    }

    private void filterPicture()
    {
        if(iv_imatge.getColorFilter() == null)
        {
            iv_imatge.setColorFilter(ContextCompat.getColor(this, R.color.GrocTransparent));
        }
        else
        {
            iv_imatge.setColorFilter(ContextCompat.getColor(this, R.color.AquaTransparent));
        }
        //iv_imatge.setColorFilter(ContextCompat.getColor(this, R.color.GrocTransparent));

        btn_save.setOnClickListener(v -> {
            if(checkPermissions())
            {
                //try {
                    //saveImageToGalery();
                    saveImage();
                /*} catch (IOException e) {
                    e.printStackTrace();
                }*/
            } else {
                askForPermissions();
            }
        });


    }

    public void rotatePicture()
    {
        if (iv_imatge.getRotation() == 0 || iv_imatge.getRotation() == 360 )
        {
            iv_imatge.setRotation(90);
        }
        else if (iv_imatge.getRotation() == 90)
        {
            iv_imatge.setRotation(180);
        }
        else if (iv_imatge.getRotation() == 180)
        {
            iv_imatge.setRotation(270);
        }
        else if (iv_imatge.getRotation() == 270)
        {
            iv_imatge.setRotation(0);
        }

    }

    private boolean checkPermissions() {
        return (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        );
    }


    private void askForPermissions() {
        ActivityCompat.requestPermissions(this, new String[]
                {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                }, 3);
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            if (intent.resolveActivity(getPackageManager()) != null) {
                activityResultLauncherGallery.launch(Intent.createChooser(intent, "Select File"));
            } else {
                Toast.makeText(MainActivity.this, "El seu dispositiu no permet accedir a la galeria",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void takePicture() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(MainActivity.this, "Error en la creació del fitxer",
                        Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "La meva foto");
                values.put(MediaStore.Images.Media.DESCRIPTION, "Foto feta el " + System.currentTimeMillis());
                Uri uriImage = FileProvider.getUriForFile(this,
                        this.getPackageName()+ ".provider", //(use your app signature + ".provider" )
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uriImage);
                activityResultLauncherPhoto.launch(intent);
            } else {
                Toast.makeText(MainActivity.this, "No s'ha pogut crear la imatge",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "El seu dispositiu no permet accedir a la càmera",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        boolean wasSuccessful; //just for testing mkdirs
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // File storageDir = getFilesDir();//no es veurà a la galeria
        // File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES+File.separator+this.getPackageName());//No es veurà a la galeria
        File storageDir =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES+File.separator+this.getPackageName());
        //NOTE: MANAGE_EXTERNAL_STORAGE is a special permission only allowed for few apps like Antivirus, file manager, etc. You have to justify the reason while publishing the app to PlayStore.
        if (!storageDir.exists()) {
            wasSuccessful =storageDir.mkdir();
        }
        else {
            wasSuccessful =storageDir.mkdirs();
        }
        if (wasSuccessful) {
            System.out.println("storageDir: " + storageDir);
        } else {
            System.out.println("storageDir: " + storageDir + " was not created");
        }
        // Save a file: path for use with ACTION_VIEW intents
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        uriPhotoImage = Uri.fromFile(image);
        System.out.println("fitxer: "+uriPhotoImage);
        return image;
    }

    private void refreshGallery() {
        //Cal refrescar per poder veure la foto creada a la galeria
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uriPhotoImage);
        this.sendBroadcast(mediaScanIntent);
    }

    public void saveImage()
    {
        try {
            //bitmap for the image
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uriPhotoImage);
            //file path to save
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
            String imageFileName = "NEW_JPEG_" + timeStamp + "_";
            //File file = new File(currentPhotoPath);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES+File.separator+this.getPackageName()), imageFileName+".jpg");
            //Fileoutputstream to write file
            FileOutputStream fOut = new FileOutputStream(file);
            //compress bitmap to specified format
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            //flush the stream
            fOut.flush();
            //close stream
            fOut.close();
            //update image to gallery
            bitmap.recycle();
            refreshGallery();
            Toast toast = Toast.makeText(this, "Imatge guardada", Toast.LENGTH_SHORT);
            toast.show();
        } catch (Exception e) {
            Toast toast = Toast.makeText(this, "Error en guardar la imatge", Toast.LENGTH_SHORT);
            toast.show();
            e.printStackTrace();
        }

    }


    /*public void saveImageToGalery() throws IOException
    {
        //get bitmap from uri
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uriPhotoImage);
        //create a file to write bitmap data
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "NEW_JPEG_" + timeStamp + "_";

        //Save image to gallery
        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                imageFileName,
                "Image of" + imageFileName
        );

        //Parse the gallery image url to uri
        File file = new File(savedImageURL);
        System.out.println("file: "+file);
        //Parse the gallery image url to uri
        Uri savedImageURI = Uri.parse(savedImageURL);
        System.out.println("uri: "+savedImageURI);
        Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
    }*/

}
