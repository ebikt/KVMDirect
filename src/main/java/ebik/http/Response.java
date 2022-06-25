package ebik.http;

import org.json.JSONObject;

public class Response {
  protected int     status_code;
  protected String  status_message;
  protected String  finalUrl;
  protected OrderedDict headers;
  protected String  response_s;
  protected byte[]  response_b;
  protected Request request;

  public Response(
      int     _status_code,
      String  _status_message,
      String  _finalUrl,
      OrderedDict _headers,
      String  _response_s,
      byte[]  _response_b,
      Request _request
    ) {
      status_code    = _status_code;
      status_message = _status_message;
      finalUrl       = _finalUrl;
      headers        = _headers;
      response_s     = _response_s;
      response_b     = _response_b;
      request        = _request;
    }

  public String url() {
    return finalUrl;
  }

  public String toString() {
    return response_s;
  }
  public byte[] data() {
    return response_b;
  }
  public int code() {
    return status_code;
  }
  public String message() {
    return status_message;
  }
  public String getHeader(String key) {
    return headers.get(key);
  }
  public OrderedDict getHeaders() {
    return headers;
  }
  public JSONObject json() {
    return new JSONObject(response_s);
  }

  public Request getRequest() {
    return request;
  }
}
