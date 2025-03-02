package tax.cute;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;

public class Main {
    public static void main(String[] args) throws IOException{
//        usage example
            MCping ping = MCping.getMotd("mc.hypixel.net", 25565);
            System.out.println("ConnectionDelay:" + ping.getDelay() + "ms");
            System.out.println("Description:" + ping.getDescription());
            System.out.println("Version:" + ping.getVersion_name());
            System.out.println("Players:" + ping.getOnline_players() + "/" + ping.getMax_players());
            System.out.println("ModList:" + ping.getModList());
            System.out.println("ModCount:" + ping.getMod_count());
            System.out.println("Type:" + ping.getType());

            boolean getFavicon = false; //You can choose whether to save the Favicon
            String saveSrc = "\\Favicon.PNG";
            if (getFavicon) Util.base64ToImage(ping.getFavicon(), saveSrc);
    }
}

class MCping {
    private String version_name;
    private String version_protocol;
    private int max_players;
    private int online_players;
    private String description;
    private String favicon;
    private String type;
    private int mod_count;
    private int delay;
    private ArrayList<String> modList;

    public MCping(
            String version_name,
            String version_protocol,
            int max_players,
            int online_players,
            String description,
            String favicon,
            String type,
            int mod_count,
            int delay,
            ArrayList<String> modList
    ) {
        this.version_name = version_name;
        this.version_protocol = version_protocol;
        this.max_players = max_players;
        this.online_players = online_players;
        this.description = description;
        this.favicon = favicon;
        this.type = type;
        this.mod_count = mod_count;
        this.modList = modList;
        this.delay = delay;
    }

    public static MCping getMotd(String host, int port) throws IOException {
        //initialization
        ArrayList<String> modList = new ArrayList<>();
        String description = "";
        JSONObject version_json;
        String version_protocol = null;
        String version_name = null;
        JSONObject players_json;
        int max_players = -1;
        int online_players = -1;
        int mod_count = 0;
        String favicon = null;
        String type = null;
        JSONObject modinfo_json;
        JSONArray modList_json;
        JSONObject description_json;

//        connection
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);

        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(byteArray);

        handshake.writeByte(0x00);
        Util.writeVarInt(handshake, 4);
        Util.writeVarInt(handshake, host.length());
        handshake.writeBytes(host);
        handshake.writeShort(port);
        Util.writeVarInt(handshake, 1);

        Util.writeVarInt(out, byteArray.size());

        out.write(byteArray.toByteArray());
        out.writeByte(0x01);
        out.writeByte(0x00);

        Util.readVarInt(in);
        Util.readVarInt(in);

        int len = Util.readVarInt(in);
        byte[] bytes = new byte[len];
        in.readFully(bytes);

        //ping get delay
        long start = System.currentTimeMillis();
        out.writeByte(0x09);
        out.writeByte(0x01);
        out.writeLong(System.currentTimeMillis());
        Util.readVarInt(in);
        Util.readVarInt(in);
        int delay = (int) (System.currentTimeMillis() - start);

        //close
        out.flush();
        out.close();
        in.close();

        String data = new String(bytes,"UTF-8");//get json
        //json parsing
        JSONObject data_json = JSONObject.parseObject(data);

        if (data_json.containsKey("version")) {
            version_json = data_json.getJSONObject("version");
            if (version_json.get("protocol") instanceof Integer) {
                version_protocol = version_json.getString("protocol");
            }
            if (version_json.get("name") instanceof String) {
                version_name = version_json.getString("name");
            }
        }

        if (data_json.containsKey("players")) {
            players_json = data_json.getJSONObject("players");
            if (players_json.get("max") instanceof Integer) {
                max_players = players_json.getIntValue("max");
            }
            if (players_json.get("online") instanceof Integer) {
                online_players = players_json.getIntValue("online");
            }
        }

        if (data_json.get("favicon") instanceof String) {
            favicon = data_json.getString("favicon").split(",")[1];
        }

        if (data_json.containsKey("modinfo")) {
            modinfo_json = data_json.getJSONObject("modinfo");
            type = modinfo_json.getString("type");
            modList_json = modinfo_json.getJSONArray("modList");
            mod_count = modList_json.size();
            for (int i = 0; i < modList_json.size(); i++) {
                modList.add(modList_json.getJSONObject(i).getString("modid"));
            }
        }

        if (data_json.containsKey("description")) {
            if (data_json.get("description") instanceof String) {
                description = data_json.getString("description");
            } else {
                description_json = data_json.getJSONObject("description");
                if (description_json.containsKey("extra")) {
                    JSONArray extra_array = description_json.getJSONArray("extra");
                    JSONObject text_json;
                    for (int i = 0; i < extra_array.size(); i++) {
                        text_json = extra_array.getJSONObject(i);
                        description += text_json.getString("text");
                    }
                } else if (description_json.containsKey("text")) {
                    description = description_json.getString("text");
                }

            }
        }

        return new MCping(
                version_name,
                version_protocol,
                max_players,
                online_players,
                description,
                favicon,
                type,
                mod_count,
                delay,
                modList
        );

    }

    public String getVersion_name() {
        return this.version_name;
    }

    public String getVersion_protocol() {
        return this.version_protocol;
    }

    public int getMax_players() {
        return this.max_players;
    }

    public int getOnline_players() {
        return this.online_players;
    }

    public String getDescription() {
        return this.description;
    }

    public String getFavicon() {
        return this.favicon;
    }

    public String getType() {
        return this.type;
    }

    public int getMod_count() {
        return this.mod_count;
    }

    public ArrayList<String> getModList() {
        return this.modList;
    }

    public int getDelay() {
        return this.delay;
    }
}

class Util {
    public static int readVarInt(DataInputStream in) throws IOException {
        int a = 0;
        int b = 0;
        while (true) {
            int c = in.readByte();

            a |= (c & 0x7F) << b++ * 7;

            if (b > 5)
                throw new RuntimeException("VarInt too big");

            if ((c & 0x80) != 128)
                break;
        }

        return a;
    }

    public static void base64ToImage(String base64, String imgSrc) throws IOException {
        OutputStream out = new FileOutputStream(imgSrc);
        byte[] bytes = Base64.getDecoder().decode(base64);
        out.write(bytes);
        out.close();
        /*
        This function receives the base64 data of the String type and the path of the String
        And generate the server favicon in the path you entered
        */
    }

    public static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }

            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }
}
