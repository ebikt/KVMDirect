package ebik.http;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

public class Request {
  private String url;
  private OrderedDict headers = null;
  private String method = null;
  private byte[] data = null;
  private OrderedDict form = null;
  private JSONObject json = null;
  private Session parent = null;
  private int timeout = -1;

  public Request(String requrl, OrderedDict hdrs, Session p) {
    url = requrl;
    headers = new OrderedDict();
    for (OrderedDict.Pair pa: hdrs) headers.add(pa);
    parent = p;
  }

  public String getUrl() {
    return url;
  }

  public OrderedDict getHeaders() {
    return headers;
  }

  public Request setHeader(String key, String value) {
    headers.set(key, value);
    return this;
  }

  public Request setTimeout(int ms) {
    timeout = ms;
    return this;
  }

  public int getTimeout() { return timeout; }

  public OrderedDict formData() {
    method = "POST";
    json = null;
    data = null;
    headers.set("Content-Type", "application/x-www-form-urlencoded");
    if (form == null)
      form = new OrderedDict();
    return form;
  }

  public Request postData(String type, String datastr) throws UnsupportedEncodingException {
    method = "POST";
    data = datastr.getBytes("utf-8");
    headers.set("Content-Type", type);
    json = null;
    form = null;
    return this;
  }

  public Request postData(JSONObject js) throws UnsupportedEncodingException {
    return this.postData("application/json", js.toString());
  }

  public JSONObject jsonData() {
    headers.set("Content-Type", "application/json");
    data = null;
    form = null;
    if (json == null) {
      json = new JSONObject();
    }
    return json;
  }

  public OrderedDict getFormData() {
    return form;
  }

  public byte[] getData() throws UnsupportedEncodingException {
    if (json != null)
      return json.toString().getBytes("utf8");
    else
      return data;
  }

  public String getMethod() {
    if (method != null)
      return method;
    else if (json == null && data == null)
      return "GET";
    else
      return "POST";
  }

  public Response getBinaryResponse() throws IOException {
    return parent.execute(this, true);
  }

  public Response getBinaryResponse(int code) throws IOException {
    Response ret = getBinaryResponse();
    if (ret.code() != code)
      throw new IOException("Server replied with status " + ret.code() + " expected: " + code);
    return ret;
  }

  public Response getResponse() throws IOException {
    return parent.execute(this, false);
  }

  public Response getResponse(int code) throws IOException {
    Response ret = getResponse();
    if (ret.code() != code)
      throw new IOException("Server replied with status " + ret.code() + " expected: " + code);
    return ret;
  }
}
