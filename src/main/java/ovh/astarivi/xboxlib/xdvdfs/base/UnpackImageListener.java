package ovh.astarivi.xboxlib.xdvdfs.base;


public interface UnpackImageListener {
    void onStep(CurrentStep step);
    void onProgress(int progress, String message);
    void onFinished();

    enum CurrentStep {
        FETCHING_ROOT_TREE,
        WALKING_FILE_TREE,
        EXTRACTING_FILES
    }
}
