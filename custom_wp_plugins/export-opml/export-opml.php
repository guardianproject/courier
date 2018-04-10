<?php
/*
Plugin Name: Export Blogroll in OPML Format
Plugin URI: http://w3.ipublicis.com/
Description: Exports your public blogroll in OPML format to a file on blog root.
Version: 1.0
Author: Lopo Lencastre de Almeida - iPublicis.com
Author URI: http://www.ipublicis.com
Donate link: http://smsh.me/7kit
License: GNU GPL v3 or later

    Copyright (C) 2010 iPublicis!COM

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*** INSTRUCTIONS **************************************************************

1. Specify the Blogroll links that you would like to export:

	a. To export ALL LINKS (default) replace 
	   the $option variable (located below) 
	   with the following line:

	   $option = "";

	b. To export ONLY PUBLIC LINKS replace 
	   the $option variable (located below) 
	   with the following line:

	   $option = "WHERE link_visible = 'Y'";

	c. To export ONLY PRIVATE LINKS replace 
	   the $option variable (located below) 
	   with the following line:

	   $option = "WHERE link_visible = 'N'";

	d. To export ONLY LINKS from a SPECIFIC CATEGORY 
	   replace the $option variable (loacted below) 
	   with the following line and edit the number
	   to match the category you want to export:

	   $option = "WHERE link_category = '1'";

2. Upload the file to WordPress plugins directory and activate via Plugins panel.

3. Go to the Options > Export Blogroll tab to grab a copy of your blogroll.

4. Save to your computer and then import to del.icio.us at your leisure!

*********************************************************************************/

/* ********************************* */
/* [ THIS IS THE VARIABLE TO EDIT: ] */
/* ********************************* */

$option = "WHERE link_visible = 'Y'";

/* *************************************** */
/* [ DO NOT EDIT BEYOND THIS POINT ]   */
/* *************************************** */

//load translation file if any for the current language
if ( !defined('WP_PLUGIN_URL') ) define( 'WP_PLUGIN_URL', get_option('siteurl') . '/wp-content/plugins');
//load_plugin_textdomain('exportopml', PLUGINDIR . '/' . $blockq_dirname . '/i18n');

function export_blogroll_admin() {
	global $wpdb, $option;

	$links = "SELECT link_url, link_name, link_rss FROM $wpdb->links  " . $option . " ORDER BY link_id DESC";
	$blogroll = $wpdb->get_results($links);

	if($blogroll){
		$opml = 	"<?xml version=\"1.0\"?>\n" .
						"<opml version=\"1.1\">\n" .
						"  <head>\n" .
						"    <title>" . get_bloginfo( 'name' ) . "</title>\n" .
						"    <dateCreated>" . date('c') . "</dateCreated>\n" . 
						"    <dateModified>" . date('c') . "</dateModified>\n" . 
						"  </head>\n" .
						"  <body>\n";

		foreach ($blogroll as $feed){
			$link_name = stripslashes($feed->link_name);
			$link_url  = stripslashes($feed->link_url);
			$link_feed = stripslashes($feed->link_rss);
			$opml .= "    <outline text=\"" . $link_name . "\" htmlUrl=\"" . $link_url . "\" " .
							"xmlUrl=\"" . $link_feed . "\" />\n";
		}

		$opml .= "  </body>\n</opml>\n";
		
		$fName = sanitize_title_with_dashes( get_bloginfo( 'name' ) ) . '.opml';
		$type = "updated";
		$msg = sprintf( __( 'New <strong>OPML</strong> done! Link to it at <strong>%s/%s</strong>.',  'exportopml' ), get_bloginfo('url'), $fName );

		$fName = get_home_path() . $fName;
		if( $fOPML = @fopen( $fName, 'w' ) ) {
			fputs( $fOPML, $opml );
			fclose( $fOPML );
		}
		else {
			$type = "error";
			$msg = sprintf( __( 'Could not write file %s', 'exportopml' ), $fName);
		}
	} else {
		$type = "error";
		$msg = __( 'Nothing to do. Thanks!',  'exportopml' );
	}

	echo "<div id=\"message\" class=\"" . $type . " fade\"><p><strong>".__( 'Export Blogroll to OPML',  'exportopml' )."</strong></p>".
					"<p>".$msg."</p></div>\n";
	//add_action( 'admin_notices', create_function( '', "echo '$notice';" ) );
}

function add_export_blogroll_admin() {
	if (function_exists('add_options_page')) {
		add_management_page(__( 'Export Blogroll to OPML',  'exportopml' ), __( 'Export Blogroll',  'exportopml' ), 8, basename(__FILE__), 'export_blogroll_admin');
	}
}
add_action('admin_menu', 'add_export_blogroll_admin');
?>
