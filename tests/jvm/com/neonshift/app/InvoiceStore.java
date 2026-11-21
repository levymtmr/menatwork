package com.neonshift.app;
import org.json.*;
/** Empty invoice source for client migration tests; production InvoiceStore is not loaded here. */
final class InvoiceStore{JSONObject draft(){return new JSONObject();}JSONArray records(){return new JSONArray();}}
