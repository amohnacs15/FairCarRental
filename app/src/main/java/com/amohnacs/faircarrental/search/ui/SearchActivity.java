package com.amohnacs.faircarrental.search.ui;

import android.app.AlertDialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amohnacs.common.mvp.MvpActivity;
import com.amohnacs.faircarrental.R;
import com.amohnacs.faircarrental.search.contracts.SearchContract;
import com.amohnacs.faircarrental.search.SearchPresenter;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.amohnacs.faircarrental.search.ui.SortingRadioDialog.SORTING_DIALOG_SELECTION;

public class SearchActivity extends MvpActivity<SearchPresenter, SearchContract.View> implements SearchContract.View,
        DatePickerDialog.OnDateSetListener, SearchResultsFragment.OnListFragmentInteractionListener,
        SortingRadioDialog.SortingDialogCallback {
    private static final String TAG = SearchActivity.class.getSimpleName();

    public static final String PICKUP_DIALOG = "pickup_dialog";
    public static final String DROPOFF_DIALOG = "dropoff_dialog";

    private static final String SAVED_INSTANCE_STATE_DIALOG_POSITION = "saved_instance_state_dialog_position";
    private static final String SAVED_INSTANCE_STATE_PICKUP_DATE = "saved_instance_state_pickup_date";
    private static final String SAVED_INSTANCE_STATE_DROP_OFF_DATE = "saved_instance_state_dropoff_date";

    @BindView(R.id.address_wrapper)
    TextInputLayout addressWrapper;

    @BindView(R.id.address_editText)
    EditText addressEditText;

    @BindView(R.id.pickup_button)
    Button pickupButton;

    @BindView(R.id.dropoff_button)
    Button dropoffButton;

    @BindView(R.id.pickup_result_textView)
    TextView pickUpResultTextView;

    @BindView(R.id.dropoff_result_textView)
    TextView dropOffResultTextView;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.search_fab)
    FloatingActionButton searchFloatingActionButton;

    @BindView(R.id.app_bar)
    AppBarLayout appBarLayout;

    private SearchPresenter presenter;
    private SearchResultsFragment searchResultFragment;

    private Drawable colorChangingMenuIcon;

    private Calendar calendar;
    private boolean datePickerActive = false; //datepickerdialog is a little laggy
    private int sortingIndex = 1;

    private String pickupString;
    private String dropOffString;

    private int scrollRange;
    private boolean isShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        ButterKnife.bind(this);
        presenter = SearchPresenter.getInstance(this);

        if (savedInstanceState != null) {
            sortingIndex = savedInstanceState.getInt(SAVED_INSTANCE_STATE_DIALOG_POSITION);
            pickupString = savedInstanceState.getString(SAVED_INSTANCE_STATE_PICKUP_DATE);
            dropOffString = savedInstanceState.getString(SAVED_INSTANCE_STATE_DROP_OFF_DATE);

            pickUpResultTextView.setText(pickupString);
            dropOffResultTextView.setText(dropOffString);
        }

        SearchResultsFragment fragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentByTag(SearchResultsFragment.TAG);
        if (fragment == null) {
            searchResultFragment = SearchResultsFragment.newInstance(1);
            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            tr.replace(R.id.fragment_container, searchResultFragment, SearchResultsFragment.TAG);
            tr.addToBackStack(null);
            tr.commit();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.toolbar_layout);

        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(R.color.transparent));
        collapsingToolbarLayout.setCollapsedTitleTextColor(getResources().getColor(R.color.white));

        calendar = Calendar.getInstance();

        pickupButton.setOnClickListener((view) -> {
            showDatePickerDialog(PICKUP_DIALOG);
        });

        dropoffButton.setOnClickListener((view) -> {
            if (!datePickerActive) {
                showDatePickerDialog(DROPOFF_DIALOG);
                datePickerActive = true;
            }
        });

        searchFloatingActionButton.setOnClickListener((view) -> {
            if (!datePickerActive) {
                presenter.validateInputsForSearch();
                datePickerActive = true;
            }
        });

        addressEditText.addTextChangedListener(new SearchTextValidator(addressEditText));

        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (scrollRange == -1) {
                scrollRange = appBarLayout.getTotalScrollRange();
            }
            //change the color of the menu icon
            if (colorChangingMenuIcon != null) {
                //scrollRange + verticalOffset == 0 is fully collapsed
                if (scrollRange + verticalOffset > -600) {
                    //collapse appBarLayout
                    isShown = true;
                    colorChangingMenuIcon.setColorFilter(getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);
                } else if (isShown) {
                    //expanded map
                    isShown = false;
                    colorChangingMenuIcon.setColorFilter(getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_INSTANCE_STATE_DIALOG_POSITION, sortingIndex);
        outState.putString(SAVED_INSTANCE_STATE_PICKUP_DATE, pickupString);
        outState.putString(SAVED_INSTANCE_STATE_DROP_OFF_DATE, dropOffString);

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        colorChangingMenuIcon = menu.findItem(R.id.action_sort).getIcon();
        if(colorChangingMenuIcon != null) {
            colorChangingMenuIcon.mutate(); //makes mutable (opposite of immutable)
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sort) {
            android.support.v4.app.FragmentManager manager = getSupportFragmentManager();

            SortingRadioDialog alert = new SortingRadioDialog();
            Bundle b  = new Bundle();
            b.putInt(SORTING_DIALOG_SELECTION, sortingIndex < 0 ? 1 : sortingIndex);
            alert.setArguments(b);

            alert.show(manager, SortingRadioDialog.TAG);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public SearchPresenter getPresenter() {
        return presenter;
    }

    @Override
    public SearchContract.View getMvpView() {
        return this;
    }

    @Override
    public void addressValidity(boolean isValid) {
        if (isValid) {
            addressWrapper.setErrorEnabled(false);
        } else {
            try {
                addressWrapper.setError(getString(R.string.address_error));
                addressEditText.requestFocus();
            } catch (UnsupportedOperationException e) { //offset error thrown with backspaces
                e.printStackTrace();
            }
        }
    }

    @Override
    public void displayDateSelection(String dialogIdentifier, String dateString) {
        if (dialogIdentifier.equals(PICKUP_DIALOG)) {
            pickupString = dateString;
            pickUpResultTextView.setText(dateString);

            dropoffButton.setCompoundDrawablesWithIntrinsicBounds(
                    0, R.drawable.ic_calendar_primary, 0, 0);
            pickupButton.setCompoundDrawablesWithIntrinsicBounds(
                    0, R.drawable.ic_calendar_today_accent, 0, 0);
        } else {
            dropOffString = dateString;
            dropOffResultTextView.setText(dateString);

            dropoffButton.setCompoundDrawablesWithIntrinsicBounds(
                    0, R.drawable.ic_calendar_accent, 0, 0);
        }
    }

    @Override
    public void searchParamError(@StringRes int messageRes) {
        datePickerActive = false;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(messageRes)
                .setTitle(R.string.param_error_title)
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    // User clicked OK button
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    @Override
    public void validInputsLaunchFragment(String addressQueryString, String pickupSelection, String dropoffSelection) {
        SearchResultsFragment f = getDisplayedFragment();
        if (f != null) {
            f.makeCarResultsRequest(addressQueryString, pickupSelection, dropoffSelection);
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        datePickerActive = false;
        presenter.onDateSetChecker(view, year, monthOfYear, dayOfMonth);
    }

    @Override
    public void resultsLoading(boolean setLoadingIndicator) {
        progressBar.setVisibility(setLoadingIndicator ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPositiveDialogClick(int position) {
        //they actually changed their sorting selection
        if (sortingIndex != position) {
            sortingIndex = position;
            SearchResultsFragment f = getDisplayedFragment();
            if (f != null) {
                f.onPositiveDialogClick(sortingIndex);
            }
        }
    }

    private void showDatePickerDialog(String tag) {
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.setOnCancelListener(dialog -> datePickerActive = false);
        datePickerDialog.setOnDismissListener(dialog -> datePickerActive = false);
        datePickerDialog.setAccentColor(ContextCompat.getColor(this, R.color.colorPrimary));
        datePickerDialog.vibrate(true);
        datePickerDialog.dismissOnPause(true);

        datePickerDialog.show(getFragmentManager(), tag);
    }

    public SearchResultsFragment getDisplayedFragment() {
        SearchResultsFragment fragment = (SearchResultsFragment) getSupportFragmentManager().findFragmentByTag(SearchResultsFragment.TAG);
        getSupportFragmentManager().executePendingTransactions();
        return fragment;
    }

    /**
     * When the user has finished entering data in edit text we send the string to the presenter
     * which ensures the string is not empty
     */
    public class SearchTextValidator implements TextWatcher {

        private View view;

        public SearchTextValidator(View view) {
            this.view = view;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //no op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //no op
        }

        @Override
        public void afterTextChanged(Editable s) {
            switch (view.getId()) {
                case R.id.address_editText:
                    String addressString = addressEditText.getText().toString().trim();
                    presenter.validateAddress(addressString);
                    break;
            }
        }
    }
}
