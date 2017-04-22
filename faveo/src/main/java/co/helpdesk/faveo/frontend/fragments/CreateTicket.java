package co.helpdesk.faveo.frontend.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import co.helpdesk.faveo.Helper;
import co.helpdesk.faveo.Preference;
import co.helpdesk.faveo.R;
import co.helpdesk.faveo.Utils;
import co.helpdesk.faveo.backend.api.v1.Helpdesk;
import co.helpdesk.faveo.frontend.activities.MainActivity;
import co.helpdesk.faveo.frontend.activities.SplashActivity;
import co.helpdesk.faveo.frontend.receivers.InternetReceiver;

public class CreateTicket extends Fragment {

    EditText editTextEmail, editTextLastName, editTextFirstName, editTextPhone, editTextSubject, editTextMessage;
    TextView textViewErrorEmail, textViewErrorLastName, textViewErrorFirstName, textViewErrorPhone, textViewErrorSubject, textViewErrorMessage;

    Spinner spinnerHelpTopic, spinnerSLAPlans, spinnerAssignTo, spinnerPriority, spinnerCountryCode;
    Button buttonSubmit;

    ArrayAdapter<String> spinnerSlaArrayAdapter, spinnerAssignToArrayAdapter,
            spinnerHelpArrayAdapter, spinnerDeptArrayAdapter, spinnerPriArrayAdapter;

    ProgressDialog progressDialog;
    int paddingTop, paddingBottom;
    View rootView;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public static CreateTicket newInstance(String param1, String param2) {
        CreateTicket fragment = new CreateTicket();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public CreateTicket() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    public int GetCountryZipCode() {
        String CountryID = "";
        String CountryZipCode = "";
        int code = 0;

        TelephonyManager manager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        //getNetworkCountryIso
        CountryID = manager.getSimCountryIso().toUpperCase();
        String[] rl = this.getResources().getStringArray(R.array.spinnerCountryCodes);
        for (int i = 0; i < rl.length; i++) {
            String[] g = rl[i].split(",");
            if (g[1].trim().equals(CountryID.trim())) {
                CountryZipCode = g[0];
                code = i;
                break;
            }
        }
        return code;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(co.helpdesk.faveo.R.layout.fragment_create_ticket, container, false);
            setUpViews(rootView);
            buttonSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetViews();
                    String email = editTextEmail.getText().toString();
                    String fname = editTextFirstName.getText().toString();
                    String lname = editTextLastName.getText().toString();
                    String phone = editTextPhone.getText().toString();
                    String subject = editTextSubject.getText().toString();
                    String message = editTextMessage.getText().toString();
                    int helpTopic = spinnerHelpTopic.getSelectedItemPosition() + 1;
                    int SLAPlans = spinnerSLAPlans.getSelectedItemPosition() + 1;
                    int assignTo = spinnerAssignTo.getSelectedItemPosition() + 1;
                    int priority = spinnerPriority.getSelectedItemPosition() + 1;
                    String countrycode = spinnerCountryCode.getSelectedItem().toString();
                    String[] cc = countrycode.split(",");
                    countrycode = cc[0];
                    boolean allCorrect = true;

                    if (email.trim().length() == 0 || !Helper.isValidEmail(email)) {
                        setErrorState(editTextEmail, textViewErrorEmail, "Email Invalido");
                        allCorrect = false;
                    }

                    if (fname.trim().length() == 0) {
                        setErrorState(editTextFirstName, textViewErrorFirstName, "Falta Nombre");
                        allCorrect = false;
                    } else if (fname.trim().length() < 3) {
                        setErrorState(editTextFirstName, textViewErrorFirstName, "Nombre debe tener al menos 3 caracteres");
                        allCorrect = false;
                    }
                    if (lname.trim().length() == 0) {
                        setErrorState(editTextLastName, textViewErrorLastName, "Falta Apellido");
                        allCorrect = false;
                    }

                    if (subject.trim().length() == 0) {
                        setErrorState(editTextSubject, textViewErrorSubject, "Por favor llena todos los campos");
                        allCorrect = false;
                    } else if (subject.trim().length() < 5) {
                        setErrorState(editTextSubject, textViewErrorSubject, "Asunto debe contener al menos 5 caracteres");
                        allCorrect = false;
                    }

                    if (message.trim().length() == 0) {
                        setErrorState(editTextMessage, textViewErrorMessage, "Por favor llena todos los campos");
                        allCorrect = false;
                    } else if (message.trim().length() < 10) {
                        setErrorState(editTextMessage, textViewErrorMessage, "Mensaje debe contener 10 caracteres");
                        allCorrect = false;
                    }

                    if (spinnerAssignTo.getSelectedItemPosition() == 1) {
                        Toast.makeText(getActivity(), "Asignacion Invalida", Toast.LENGTH_LONG).show();
                        setErrorState(editTextMessage, textViewErrorMessage, "Asignacion Invalida");
                        allCorrect = false;
                    }

                    if (allCorrect) {
                        if (InternetReceiver.isConnected()) {
                            progressDialog = new ProgressDialog(getActivity());
                            progressDialog.setMessage("Creando prioridad");
                            progressDialog.show();
                            try {
                                fname = URLEncoder.encode(fname, "utf-8");
                                lname = URLEncoder.encode(lname, "utf-8");
                                subject = URLEncoder.encode(subject, "utf-8");
                                message = URLEncoder.encode(message, "utf-8");
                                email = URLEncoder.encode(email, "utf-8");
                                phone = URLEncoder.encode(phone, "utf-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            new CreateNewTicket(Integer.parseInt(Preference.getUserID()), subject, message, helpTopic, SLAPlans, priority, assignTo, phone, fname, lname, email, countrycode).execute();

                        } else
                            Toast.makeText(v.getContext(), "Oops! No hay internet", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        ((MainActivity) getActivity()).setActionBarTitle("Crear Prioridad");
        return rootView;
    }

    public class CreateNewTicket extends AsyncTask<String, Void, String> {
        int userID;
        String phone;
        String subject;
        String body;
        String fname, lname, email;
        int helpTopic;
        int SLA;
        int priority;
        int dept;
        String code;

        CreateNewTicket(int userID, String subject, String body,
                        int helpTopic, int SLA, int priority, int dept, String phone, String fname, String lname, String email, String code) {
            this.userID = userID;
            this.subject = subject;
            this.body = body;
            this.helpTopic = helpTopic;
            this.SLA = SLA;
            this.priority = priority;
            this.dept = dept;
            this.phone = phone;
            this.lname = lname;
            this.fname = fname;
            this.email = email;
            this.code = code;
        }

        protected String doInBackground(String... urls) {
            return new Helpdesk().postCreateTicket(userID, subject, body, helpTopic, SLA, priority, dept, fname, lname, phone, email, code);
        }

        protected void onPostExecute(String result) {
            Log.d("result  crear prioridad:", result + "");
            progressDialog.dismiss();
            if (result == null) {
                Toast.makeText(getActivity(), "Se ha provocado un error", Toast.LENGTH_LONG).show();
                return;
            }
            if (result.contains("Prioridad creada exitosamente!")) {
                Toast.makeText(getActivity(), "Prioridad creada", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void resetViews() {
        editTextEmail.setBackgroundResource(co.helpdesk.faveo.R.drawable.edittext_theme_states);
        editTextEmail.setPadding(0, paddingTop, 0, paddingBottom);
        editTextFirstName.setBackgroundResource(co.helpdesk.faveo.R.drawable.edittext_theme_states);
        editTextFirstName.setPadding(0, paddingTop, 0, paddingBottom);
        editTextLastName.setBackgroundResource(co.helpdesk.faveo.R.drawable.edittext_theme_states);
        editTextLastName.setPadding(0, paddingTop, 0, paddingBottom);
        editTextPhone.setBackgroundResource(co.helpdesk.faveo.R.drawable.edittext_theme_states);
        editTextPhone.setPadding(0, paddingTop, 0, paddingBottom);
        editTextSubject.setBackgroundResource(co.helpdesk.faveo.R.drawable.edittext_theme_states);
        editTextSubject.setPadding(0, paddingTop, 0, paddingBottom);
        editTextMessage.setBackgroundResource(co.helpdesk.faveo.R.drawable.edittext_theme_states);
        editTextMessage.setPadding(0, paddingTop, 0, paddingBottom);
        textViewErrorEmail.setText("");
        textViewErrorFirstName.setText("");
        textViewErrorLastName.setText("");
        // textViewErrorPhone.setText("");
        textViewErrorSubject.setText("");
        textViewErrorMessage.setText("");

    }

    private void setErrorState(EditText editText, TextView textViewError, String error) {
        editText.setBackgroundResource(co.helpdesk.faveo.R.drawable.edittext_error_state);
        editText.setPadding(0, paddingTop, 0, paddingBottom);
        textViewError.setText(error);
    }

    private void setUpViews(View rootView) {
        editTextEmail = (EditText) rootView.findViewById(co.helpdesk.faveo.R.id.editText_email);
        editTextEmail.setText(Preference.getEmail());
        editTextFirstName = (EditText) rootView.findViewById(co.helpdesk.faveo.R.id.editText_firstname);
        editTextLastName = (EditText) rootView.findViewById(co.helpdesk.faveo.R.id.editText_lastname);
        editTextPhone = (EditText) rootView.findViewById(co.helpdesk.faveo.R.id.editText_phone);
        // editTextPhone.setText("91");
        editTextSubject = (EditText) rootView.findViewById(co.helpdesk.faveo.R.id.editText_subject);
        editTextMessage = (EditText) rootView.findViewById(co.helpdesk.faveo.R.id.editText_message);
        textViewErrorEmail = (TextView) rootView.findViewById(co.helpdesk.faveo.R.id.textView_error_email);
        textViewErrorFirstName = (TextView) rootView.findViewById(co.helpdesk.faveo.R.id.textView_error_firstname);
        textViewErrorLastName = (TextView) rootView.findViewById(co.helpdesk.faveo.R.id.textView_error_lastname);
        textViewErrorPhone = (TextView) rootView.findViewById(co.helpdesk.faveo.R.id.textView_error_phone);
        textViewErrorSubject = (TextView) rootView.findViewById(co.helpdesk.faveo.R.id.textView_error_subject);
        textViewErrorMessage = (TextView) rootView.findViewById(co.helpdesk.faveo.R.id.textView_error_message);

        spinnerHelpTopic = (Spinner) rootView.findViewById(co.helpdesk.faveo.R.id.spinner_help_topics);
        spinnerHelpArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Utils.removeDuplicates(SplashActivity.valueTopic.split(","))); //selected item will look like a spinner set from XML
        spinnerHelpArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHelpTopic.setAdapter(spinnerHelpArrayAdapter);

        spinnerSLAPlans = (Spinner) rootView.findViewById(co.helpdesk.faveo.R.id.spinner_sla_plans);
        spinnerSlaArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Utils.removeDuplicates(SplashActivity.valueSLA.split(","))); //selected item will look like a spinner set from XML
        spinnerSlaArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSLAPlans.setAdapter(spinnerSlaArrayAdapter);

        spinnerAssignTo = (Spinner) rootView.findViewById(co.helpdesk.faveo.R.id.spinner_assign_to);
        spinnerAssignToArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Utils.removeDuplicates(SplashActivity.valueDepartment.split(","))); //selected item will look like a spinner set from XML
        spinnerAssignToArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssignTo.setAdapter(spinnerAssignToArrayAdapter);

        spinnerPriority = (Spinner) rootView.findViewById(co.helpdesk.faveo.R.id.spinner_priority);
        spinnerPriArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, Utils.removeDuplicates(SplashActivity.valuePriority.split(","))); //selected item will look like a spinner set from XML
        spinnerPriArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(spinnerPriArrayAdapter);

        spinnerCountryCode = (Spinner) rootView.findViewById(R.id.spinner_code);
        spinnerCountryCode.setSelection(GetCountryZipCode());
        buttonSubmit = (Button) rootView.findViewById(co.helpdesk.faveo.R.id.button_submit);
        paddingTop = editTextEmail.getPaddingTop();
        paddingBottom = editTextEmail.getPaddingBottom();
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

}
