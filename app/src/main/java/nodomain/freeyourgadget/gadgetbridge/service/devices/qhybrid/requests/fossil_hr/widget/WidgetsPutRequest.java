package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FilePutRawRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;

public class WidgetsPutRequest extends JsonPutRequest {
    public WidgetsPutRequest(Widget[] widgets, FossilWatchAdapter adapter) {
        super((short) 0x0501, prepareFile(widgets), adapter);
    }

    private static JSONObject prepareFile(Widget[] widgets){
        try {
            JSONArray widgetArray = new JSONArray(widgets);

            JSONObject object = new JSONObject()
                    .put(
                            "push",
                            new JSONObject()
                            .put("set",
                                new JSONObject().put(
                                        "watchFace._.config.comps", widgetArray
                                )
                            )
                    );
            return object;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
