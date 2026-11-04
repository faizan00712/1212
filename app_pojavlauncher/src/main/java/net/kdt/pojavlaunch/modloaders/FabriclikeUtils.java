package net.kdt.pojavlaunch.modloaders;

import com.google.gson.JsonSyntaxException;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FabriclikeUtils {

    public static final FabriclikeUtils FABRIC_UTILS = new FabriclikeUtils("https://meta.fabricmc.net/v2", "https://bmclapi2.bangbang93.com/fabric-meta/v2", "fabric", "Fabric", "fabric");
    public static final FabriclikeUtils QUILT_UTILS = new FabriclikeUtils("https://meta.quiltmc.org/v3", "https://bmclapi2.bangbang93.com/quilt-meta/v3", "quilt", "Quilt", "quilt");
    public static final FabriclikeUtils LEGACY_FABRIC_UTILS = new FabriclikeUtils("https://meta.legacyfabric.net/v2", null, "legacy_fabric", "Legacy Fabric", "fabric");

    private static final String JSON_DOWNLOAD_URL = "%s/versions/loader/%s/%s/profile/json";

    private final String mApiUrl;
    private final String mMirrorUrl;
    private final String mCachePrefix;
    private final String mName;
    private final String mIconName;

    private FabriclikeUtils(String mApiUrl, String mirrorUrl, String cachePrefix, String mName, String iconName) {
        this.mApiUrl = mApiUrl;
        this.mMirrorUrl = mirrorUrl;
        this.mCachePrefix = cachePrefix;
        this.mIconName = iconName;
        this.mName = mName;
    }

    public FabricVersion[] downloadGameVersions() throws IOException{
        try {
            return cachedWithMirror("/versions/game", mCachePrefix+"_game_versions",
                    FabriclikeUtils::deserializeRawVersions
            );
        }catch (DownloadUtils.ParseException ignored) {}
        return null;
    }

    public FabricVersion[] downloadLoaderVersions(String gameVersion) throws IOException{
        try {
            String urlEncodedGameVersion = URLEncoder.encode(gameVersion, "UTF-8");
            return cachedWithMirror("/versions/loader/" + urlEncodedGameVersion,
                    mCachePrefix+"_loader_versions."+urlEncodedGameVersion,
                    (input)->{ try {
                        return deserializeLoaderVersions(input);
                    }catch (JSONException e) {
                        throw new DownloadUtils.ParseException(e);
                    }});

        }catch (DownloadUtils.ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> T cachedWithMirror(String path, String cacheName, DownloadUtils.ParseCallback<T> parser)
            throws IOException, DownloadUtils.ParseException {
        try {
            return DownloadUtils.downloadStringCached(mApiUrl + path, cacheName, parser);
        } catch (IOException e) {
            if (mMirrorUrl == null) throw e;
            return DownloadUtils.downloadStringCached(mMirrorUrl + path, cacheName, parser);
        }
    }

    private String downloadJson(String gameVersion, String loaderVersion) throws IOException {
        String[] bases = mMirrorUrl == null ? new String[]{mApiUrl, mApiUrl} : new String[]{mApiUrl, mApiUrl, mMirrorUrl};
        IOException last = null;
        for (int i = 0; i < bases.length; i++) {
            try {
                return DownloadUtils.downloadString(createJsonDownloadUrl(bases[i], gameVersion, loaderVersion));
            } catch (IOException e) {
                last = e;
                if (i + 1 < bases.length) {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Install interrupted", interrupted);
                    }
                }
            }
        }
        throw last;
    }

    public String createJsonDownloadUrl(String gameVersion, String loaderVersion) {
        return createJsonDownloadUrl(mApiUrl, gameVersion, loaderVersion);
    }

    private String createJsonDownloadUrl(String apiUrl, String gameVersion, String loaderVersion) {
        try {
            gameVersion = URLEncoder.encode(gameVersion, "UTF-8");
            loaderVersion = URLEncoder.encode(loaderVersion, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return String.format(JSON_DOWNLOAD_URL, apiUrl, gameVersion, loaderVersion);
    }

    public String getName() {
        return mName;
    }
    public String getIconName() {
        return mIconName;
    }

    public String install(String gameVersion, String loaderVersion) throws IOException {
        String fabricJson = downloadJson(gameVersion, loaderVersion);
        String versionId;
        try {
            JSONObject fabricJsonObject = new JSONObject(fabricJson);
            versionId = fabricJsonObject.getString("id");
        }catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        File versionJsonDir = new File(Tools.DIR_HOME_VERSION, versionId);
        File versionJsonFile = new File(versionJsonDir, versionId+".json");
        FileUtils.ensureDirectory(versionJsonDir);
        Tools.write(versionJsonFile, fabricJson);
        return versionId;
    }

    private static FabricVersion[] deserializeLoaderVersions(String input) throws JSONException {
        JSONArray jsonArray = new JSONArray(input);
        FabricVersion[] fabricVersions = new FabricVersion[jsonArray.length()];
        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i).getJSONObject("loader");
            FabricVersion fabricVersion = new FabricVersion();
            fabricVersion.version = jsonObject.getString("version");
            //Quilt has a skill issue and does not say which versions are stable or not
            if(jsonObject.has("stable")) {
                fabricVersion.stable = jsonObject.getBoolean("stable");
            } else {
                fabricVersion.stable = !fabricVersion.version.contains("beta");
            }
            fabricVersions[i] = fabricVersion;
        }
        return fabricVersions;
    }

    private static FabricVersion[] deserializeRawVersions(String jsonArrayIn) throws DownloadUtils.ParseException {
        try {
            return Tools.GLOBAL_GSON.fromJson(jsonArrayIn, FabricVersion[].class);
        }catch (JsonSyntaxException e) {
            e.printStackTrace();
            throw new DownloadUtils.ParseException(null);
        }
    }
}
