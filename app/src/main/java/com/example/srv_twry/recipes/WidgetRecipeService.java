package com.example.srv_twry.recipes;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by srv_twry on 6/6/17.
 */

public class WidgetRecipeService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RecipeRemoteViewFactory(this.getApplicationContext(),intent);
    }

    private class RecipeRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory{

        private final String TAG = RecipeRemoteViewFactory.class.getSimpleName();
        private Context context;
        private ArrayList<Recipe> recipeArrayList;
        private ArrayList<Ingredient> ingredientsList = new ArrayList<Ingredient>();
        private AppWidgetManager appWidgetManager;
        private int appWidgetId;

        public RecipeRemoteViewFactory(Context context , Intent intent){
            recipeArrayList = new ArrayList<>();
            this.context = context;
            appWidgetManager = AppWidgetManager.getInstance(context);
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        @Override
        public void onCreate() {
            //Make the http request here
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            if (isNetworkAvailable()){
                String url = getResources().getString(R.string.url);
                JsonArrayRequest jsonArrayRequest = getJsonArray(url);
                requestQueue.add(jsonArrayRequest);
            }
            Log.v(TAG,"On create");
        }

        private JsonArrayRequest getJsonArray(String url) {
            return new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    recipeArrayList = parseRecipeJson(response);
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId,R.id.widget_stackView);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG,"Volley error response");
                }
            });
        }

        private ArrayList<Recipe> parseRecipeJson(JSONArray response) {
            ArrayList<Recipe> returnedRecipeList= new ArrayList<>();

            if (response !=null){
                try{
                    int recipeId;
                    String name;
                    ArrayList<Ingredient> ingredients;
                    ArrayList<Steps> steps;
                    int servings;

                    for (int i=0;i<response.length();i++){
                        JSONObject obj = response.getJSONObject(i);
                        recipeId = obj.getInt("id");
                        name = obj.getString("name");
                        ingredients = new ArrayList<>();
                        steps = new ArrayList<>();
                        JSONArray ingredientsJsonArray = obj.getJSONArray("ingredients");
                        JSONArray stepsJsonArray = obj.getJSONArray("steps");
                        servings = obj.getInt("servings");

                        //Creating Arraylist out of JSONArray
                        for (int j=0;j<ingredientsJsonArray.length();j++){
                            JSONObject ingredientsObject = ingredientsJsonArray.getJSONObject(j);
                            double quantity = ingredientsObject.getDouble("quantity");
                            String measure = ingredientsObject.getString("measure");
                            String ingredient = ingredientsObject.getString("ingredient");
                            ingredients.add(new Ingredient(quantity,measure,ingredient));
                        }

                        for (int j=0;j<stepsJsonArray.length();j++){
                            JSONObject stepsJsonObject = stepsJsonArray.getJSONObject(j);
                            int stepId= stepsJsonObject.getInt("id");
                            String shortDescription = stepsJsonObject.getString("shortDescription");
                            String description = stepsJsonObject.getString("description");
                            String videoUrl = stepsJsonObject.getString("videoURL");
                            String thumbnailUrl = stepsJsonObject.getString("thumbnailURL");
                            steps.add(new Steps(stepId,shortDescription,description,videoUrl,thumbnailUrl));
                        }
                        Log.v("ingredients "+i,ingredients.size()+" ");
                        Log.v("steps "+i,steps.size()+" ");
                        returnedRecipeList.add(new Recipe(recipeId,name,ingredients,steps,servings));
                    }
                }catch (JSONException e){
                    e.printStackTrace();

                }
            }
            return returnedRecipeList;
        }

        @Override
        public void onDataSetChanged() {
            Log.v(TAG,"Data changed");
        }

        @Override
        public void onDestroy() {
            recipeArrayList.clear();
            ingredientsList.clear();
        }

        @Override
        public int getCount() {
            return recipeArrayList.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            //Add the data to individual item here.
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.widget_stackview_item_layout);
            if (recipeArrayList !=null){
                ingredientsList = recipeArrayList.get(position).ingredientsArrayList;
                StringBuilder ingredientDescription = new StringBuilder();
                for (Ingredient ingredient: ingredientsList){
                    ingredientDescription.append(" * ");
                    ingredientDescription.append(ingredient.quantity);
                    ingredientDescription.append(" ");
                    ingredientDescription.append(ingredient.measure);
                    ingredientDescription.append(" - ");
                    ingredientDescription.append(ingredient.ingredients);
                    ingredientDescription.append("\n");
                }
                remoteViews.setTextViewText(R.id.widget_stackview_recipe_title,recipeArrayList.get(position).name);
                remoteViews.setTextViewText(R.id.widget_stackView_recipe_ingredients,ingredientDescription);
                Bundle bundle = new Bundle();
                bundle.putInt(RecipeWidgetProvider.EXTRA_INGREDIENT_TAG, position);
                Intent fillInIntent = new Intent();
                fillInIntent.putExtras(bundle);
            }else {
                Log.v(TAG,"Recipe list null");
            }
            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        //A helper method to check Internet Connection Status
        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
}
