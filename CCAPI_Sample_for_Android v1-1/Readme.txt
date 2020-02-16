*Exaplanation of Camera Control API(CCAPI) sample program

+ Sample
  + app
    + src
      + main
        - AndroidManifest.xml : Manifest file
      + assets
        - settings.json	: List of setting values to be displayed on the ListView.
      + java
	- APIDataSet.java				: The class which manages APIs supported by Canon cameras.
	- AuthenticateDialogFragment.java		: The DialogFragment to be displayed when an digest authentication error occurs.
	- AuthenticateListener.java			: The Listener for the callback to the Activity which displays AuthenticateDialogFragment.
	- ChunkResultListener.java			: The Listener for the callback when reading chunk data.
	- Constants.java				: The class of constants.
	- ContentsAdapter.java				: The Adapter to connect the GridView which displays the list of content thumbnail images and the ContentsDataSet.
	- ContentsDataSet.java				: The class which manages the information of contents.
	- ContentsDownloader.java			: The class of the contents saving function.
	- ContentsEditDialogFragment.java		: The DialogFragment which displays a selection of contents operations.
	- ContentsInfoDialogFragment.java		: The DialogFragment which displays contents information.
	- ContentsShowFragment.java			: The Fragment which displays contents.
	- ContentsSpinnerDialogFragment.java		: The DialogFragment for selecting a parameter by a drop-down when executing a contents operation.
	- ContentsViewerFragment.java			: The Fragment which displays the Contents Viewer screen.
	- CustomSimpleAdapter.java			: The Adapter to connect the ListView and setting values which display in the Remote Capture screen.
	- CustomSimpleExpandableListAdapter.java	: The Adapter to connect the ListView and the ListViewDataSet of the ListViewFragment.
	- DateTimeSettingDialogFragment.java		: The DialogFragment which displays a dialog of the date setting.
	- DisconnectListener.java			: The Listener which defines the interface to notify of the application end processing.
	- EventListener.java				: The Listener which defines the interface to notify of the event data.
	- EventThread.java				: The Thread for the event acquisition.
	- HttpCommunication.java			: The class for the HTTP communication processing.
	- HttpDigestAuth.java				: The class which manages the header information of the digest authentication.
	- HttpProgressListener.java			: The Listener which defines interface to notify of the receiving of the HTTP response body.
	- HttpResultDataSet.java			: The class which stores a result of the HTTP communication.
	- ImageProcessor.java				: The class which performs the memory management of content thumbnail images.
	- ListViewDataSet.java				: The class which manages setting values for displaying in the ListView.
	- ListViewDialogFragment.java			: The DialogFragment which displays the dialog for setting values when selected the ListView.
	- ListViewFragment.java				: The Fragment which displays the Device Information / the Camera Functions screen.
	- LiveViewThread.java				: The Thread which performs the acquisition / update of the Live View.
	- MainActivity.java				: The Activity which displays the top screen.
	- MessageDialogFragment.java			: The DialogFragment which displays message.
	- OrgFormatDataSet.java				: The class which manages / parses data of the Live View and event.
	- ProgressDialogFragment.java			: The DialogFragment which displays the progress.
	- RemoteCaptureFragment.java			: The Fragment which displays the Remote Capture screen.
	- SubActivity.java				: The Activity which displays the selected screen in the top screen.
	- WebAPI.java					: The class which manages execution of API.
	- WebAPIQueueDataSet.java			: The class which defines the cue to give a execution request to WebAPI.
	- WebAPIResultDataSet.java			: The class which stores a result of API execution.
	- WebAPIResultListener.java			: The Listener which defines interface to receive a result of API execution.
	- WifiConnection.java				: The class which performs a device discovery.
	- WifiMonitoringThread.java			: The Thread which manages the connection status.
	- WifiSettingDialogFragment.java		: The DialogFragment which displays a dialog for the wireless settings.
      + res
        + drawable
          - border.xml : The resource file which displays the section line on GUI.
        + layout
          - activity_main.xml					: The resource file which displays the MainActivity.
          - activity_sub.xml					: The resource file which displays the SubActivity.
          - expandable_list_view_small_child_item_layout.xml	: The resource file which defines the layout for the list of ListViewFragment's setting values.
          - expandable_list_view_small_parent_item_layout.xml	: The resource file which defines the layout for the list of ListViewFragment's setting values.
          - fragment_contents_viewer.xml			: The resource file which displays the Contents Viewer.
          - fragment_contents_viewer_image.xml			: The resource file which displays contents.
          - fragment_dialog_authenticate.xml			: The resource file of the dialog for inputting user name and password of the digest authentication.
          - fragment_dialog_date_time_setting.xml		: The resource file of the dialog for inputting the date settings.
          - fragment_dialog_progress_bar.xml			: The resource file of the dialog for displaying progress.
          - fragment_dialog_progress_circular.xml		: The resource file of the dialog for displaying progress.
          - fragment_dialog_wifi_setting.xml			: The resource file of the dialog for inputting the wireless settings.
          - fragment_list_view.xml				: The resource file which displays the Device Information / the Camera Functions.
          - fragment_remote_capture.xml				: The resource file which displays the Remote Capture.
          - grid_contents.xml					: The resource file which defines the layout of file names and content thumbnail images.
          - list_view_child_item_layout.xml			: The resource file to use in CustomSimpleExpandableListAdapter.
          - list_view_parent_item_layout.xml			: The resource file to use in CustomSimpleExpandableListAdapter.
          - list_view_small_item_layout.xml			: The resource file which defines the layout for the list of Remote Capture's Settings.
        + menu
          - activity_fragment_main_drawer.xml : The resource file which defines the layout of the optional menu of SubActivity.
        + values
          - colors.xml	: The resource file which defines the color code.
          - dimens.xml	: The resource file which defines such as margins.
          - strings.xml	: The resource file which defines the string to appoint on other resource files.
        + xml
          - file_path.xml : The resource file which defines the destination when saving contents.
