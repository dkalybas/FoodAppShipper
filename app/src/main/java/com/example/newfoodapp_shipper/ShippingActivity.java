package com.example.newfoodapp_shipper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.paperdb.Paper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.newfoodapp_shipper.Common.Common;
import com.example.newfoodapp_shipper.Common.LatLngInterpolator;
import com.example.newfoodapp_shipper.Common.MarkerAnimation;
import com.example.newfoodapp_shipper.model.ShippingOrderModel;
import com.example.newfoodapp_shipper.remote.IGoogleAPI;
import com.example.newfoodapp_shipper.remote.RetrofitClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.objectweb.asm.Handle;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

public class ShippingActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private Marker shipperMarker;
    private ShippingOrderModel shippingOrderModel;

    @BindView(R.id.txt_order_number)
    TextView txt_order_number;
    @BindView(R.id.txt_name)
    TextView txt_name;
    @BindView(R.id.txt_address)
    TextView txt_address;
    @BindView(R.id.txt_date)
    TextView txt_date;

    @BindView(R.id.btn_start_trip)
    MaterialButton btn_start_trip;
    @BindView(R.id.btn_call)
    MaterialButton btn_call;
    @BindView(R.id.btn_done)
    MaterialButton btn_done;

    @BindView(R.id.img_food_image)
    ImageView img_food_image;

    AutocompleteSupportFragment places_fragment;
    PlacesClient placesClient;
    List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);


    @OnClick(R.id.btn_start_trip)
    void onStartTripClick(){
        String data = Paper.book().read(Common.SHIPPING_ORDER_DATA);
        Paper.book().write(Common.TRIP_START,data);
      btn_start_trip.setEnabled(false);
    }

    private boolean isInit = false;
    private Location previousLocation = null;

    //Animation
    private Handler handler;
    private int index,next;
    private LatLng start,end;
    private float v;
    private double lat,lng;
    private Polyline blackPolyline,greyPolyline;
    private PolylineOptions polylineOptions,blackPolylineOptions;
    private List<LatLng> polylineList;
    private IGoogleAPI iGoogleAPI;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipping);

        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);

        initPlaces();
        setupAutoCompletePlaces();

        ButterKnife.bind(this);
        buildLocationRequest();
        buildLocationCallBack();
        setShippingOrder();

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {


                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(ShippingActivity.this::onMapReady);

                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ShippingActivity.this);
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,Looper.myLooper());

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(ShippingActivity.this,"You must enable this location permission ",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();


    }

    private void setupAutoCompletePlaces() {

            places_fragment = (AutocompleteSupportFragment)getSupportFragmentManager()
                    .findFragmentById(R.id.places_autocomplete_fragment);
            places_fragment.setPlaceFields(placeFields);
            places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                Toast.makeText(ShippingActivity.this,new StringBuilder(place.getName())
                .append("-")
                .append(place.getLatLng().toString()),Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onError(@NonNull Status status) {
                Toast.makeText(ShippingActivity.this,""+status.getStatusMessage(),Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void initPlaces() {

        Places.initialize(this,getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);

    }

    private void setShippingOrder() {
        Paper.init(this);
        String data ;

        if (TextUtils.isEmpty(Paper.book().read(Common.TRIP_START))){

            // if it is empty  just do it normal
            btn_start_trip.setEnabled(true);
            data = Paper.book().read(Common.SHIPPING_ORDER_DATA);
        }else {

            btn_start_trip.setEnabled(false);
            data = Paper.book().read(Common.TRIP_START);


        }

        if (!TextUtils.isEmpty(data)){

            shippingOrderModel = new Gson()
                    .fromJson(data,new TypeToken<ShippingOrderModel>(){}.getType());

            if (shippingOrderModel!=null){

                Common.setSpanStringColor("Name :",
                        shippingOrderModel.getOrderModel().getUserName(),
                        txt_name,
                        Color.parseColor("#333639"));

                txt_date.setText(new StringBuilder()
                .append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                .format(shippingOrderModel.getOrderModel().getCreateDate())));

                Common.setSpanStringColor("No :",
                        shippingOrderModel.getOrderModel().getKey(),
                        txt_order_number,
                        Color.parseColor("#673ab7"));

                Common.setSpanStringColor("Address :",
                        shippingOrderModel.getOrderModel().getShippingAddress(),
                        txt_address,
                        Color.parseColor("#795548"));

                Glide.with(this)
                        .load(shippingOrderModel.getOrderModel().getCartItemList().get(0)
                        .getFoodImage())
                        .into(img_food_image);

            }


        }else{

            Toast.makeText(this,"Shipping order is null !! ",Toast.LENGTH_SHORT).show();
        }

    }

    private void buildLocationCallBack() {

        locationCallback = new LocationCallback(){

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // Add a marker in Sydney and move the camera
                LatLng locationShipper = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                if (shipperMarker==null){

                    // here we inflate drawable(delivery guy photo)

                  int height,width;
                  height = width =80;
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(ShippingActivity.this,R.drawable.shippernew);
                    Bitmap resized = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(),width,height,false);

                    shipperMarker =    mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(resized))
                            .position(locationShipper).title("You"));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,18));


                }



                if(isInit&&previousLocation!=null){

                   String from = new StringBuilder()
                           .append(previousLocation.getLatitude())
                           .append(",")
                           .append(previousLocation.getLongitude())
                           .toString();

                   String to = new StringBuilder()
                           .append(locationShipper.latitude)
                           .append(",")
                           .append(locationShipper.longitude)
                           .toString();

                    moveMarkerAnimation(shipperMarker,from,to);

                    previousLocation = locationResult.getLastLocation();
                }
                if (!isInit){

                    isInit=true;
                    previousLocation = locationResult.getLastLocation();

                }


            }
        };





    }

    private void moveMarkerAnimation(Marker marker, String from, String to) {

        //Requesting directions API to get data
            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                    "less_driving",
                    from,to,
                    getString(R.string.google_maps_key))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(returnResult -> {

                try {

                    //Parsing JSON
                    JSONObject jsonObject = new JSONObject(returnResult);
                    JSONArray jsonArray =  jsonObject.getJSONArray("routes");
                    for (int i = 0 ; i<jsonArray.length() ; i++){

                        JSONObject route = jsonArray.getJSONObject(i);
                        JSONObject poly = route.getJSONObject("overview_polyline");
                        String polyline = poly.getString("points");
                        polylineList = Common.decodePoly(polyline);

                    }
                    polylineOptions = new PolylineOptions();
                    polylineOptions.color(Color.GRAY);
                    polylineOptions.width(5);
                    polylineOptions.startCap(new SquareCap());
                    polylineOptions.jointType(JointType.ROUND);
                    polylineOptions.addAll(polylineList);
                    greyPolyline = mMap.addPolyline(polylineOptions);

                    blackPolylineOptions = new PolylineOptions();
                    blackPolylineOptions.color(Color.BLACK);
                    blackPolylineOptions.width(5);
                    blackPolylineOptions.startCap(new SquareCap());
                    blackPolylineOptions.jointType(JointType.ROUND);
                    blackPolylineOptions.addAll(polylineList);
                    blackPolyline = mMap.addPolyline(blackPolylineOptions);

                    //Animator
                    ValueAnimator polylineAnimator = ValueAnimator.ofInt(0,100);
                    polylineAnimator.setDuration(2000);
                    polylineAnimator.setInterpolator(new LinearInterpolator());
                    polylineAnimator.addUpdateListener(animation -> {

                        List<LatLng> points = greyPolyline.getPoints();
                        int percentValue = (int)animation.getAnimatedValue();
                        int size = points.size();
                        int newPoints  =  (int)(size*(percentValue/100.0f));
                        List<LatLng> p = points.subList(0,newPoints);
                        blackPolyline.setPoints(p);


                    });
                    polylineAnimator.start();

                    //Bike Moving here
                    handler = new Handler();
                    index=-1;
                    next =1 ;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (index < polylineList.size()-1){

                                index++;
                                next=index+1;
                                start = polylineList.get(index);
                                end = polylineList.get(next);
                            }
                                ValueAnimator valueAnimator = ValueAnimator.ofInt(0,1);
                                valueAnimator.setDuration(1500);
                                valueAnimator.setInterpolator(new LinearInterpolator());
                                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        v = valueAnimator.getAnimatedFraction();
                                        lng = v*end.longitude+(1-v)*start.longitude;
                                        lat=v*end.latitude+(1-v)*start.latitude;
                                        LatLng newPos = new LatLng(lat,lng);
                                        marker.setPosition(newPos);
                                        marker.setAnchor(0.5f,0.5f);
                                        marker.setRotation(Common.getBearing(start,newPos));

                                        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                                    }
                                });
                                    valueAnimator.start();
                                    if (index < polylineList.size() -2)  //Reach destination
                                        handler.postDelayed(this,1500);

                        }
                    },1500);

                }catch (Exception e ){


                }

            }, throwable -> {
                if (throwable!=null)
                    Toast.makeText(ShippingActivity.this,""+throwable.getMessage(),Toast.LENGTH_SHORT).show();
            }));



    }

    private void buildLocationRequest() {

        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(15000); // 15 seconds
        locationRequest.setFastestInterval(10000);  // 10 seconds
        locationRequest.setSmallestDisplacement(20f);// its 20 meters



    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


    }

    @Override
    protected void onDestroy() {

        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        compositeDisposable.clear();
        super.onDestroy();


    }
}
