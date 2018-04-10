<?php

/*

	Plugin Name: Syndicate Out
	Plugin URI: http://www.flutt.co.uk/development/wordpress-plugins/syndicate-out/
	Version: 0.7
	Description: Syndicates posts made in any specified category to another WordPress blog using WordPress' built in XML-RPC functionality.
	Author: ConfuzzledDuck
	Author URI: http://www.flutt.co.uk/

*/

#
#  syndicate-out.php
#
#  Created by Jonathon Wardman on 09-07-2009.
#  Copyright 2009 - 2013, Jonathon Wardman. All rights reserved.
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  You may obtain a copy of the License at:
#  http://www.gnu.org/licenses/gpl-3.0.txt
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.


	 // Nothing in this plugin works outside of the admin area, so don't bother
	 // loading it if we're not looking at the admin panel...
if ( is_admin() ) {

 /* Setup section. */

	 // Global constants and variables relating to posts...
	define( 'SO_OPTIONS_VERSION', '2' );

	 // Register functions...
	add_action( 'admin_menu', 'syndicate_out_menu' );
	add_action( 'admin_init', 'syndicate_out_register_settings' );
	add_action( 'publish_post', 'syndicate_out_post' );
	add_filter( 'plugin_action_links', 'syndicate_out_settings_link', 10, 2 );

    // Register the plugin activation and delete functions...
	register_activation_hook( __FILE__, 'syndicate_out_activate' );
	//register_uninstall_hook( __FILE__, 'syndicate_out_delete' );

 /* Admin section. */

	 // Admin menu...
	function syndicate_out_menu() {
	
		add_submenu_page( 'options-general.php', 'Syndicate Out Settings', 'Syndication', 'manage_options', 'syndicate_out', 'syndicate_out_admin' );

	}

	 // Settings link on plugins page...
	function syndicate_out_settings_link($links, $file) {

		if ( plugin_basename( __FILE__ ) == $file ) {
			array_push( $links, '<a href="options-general.php?page=syndicate_out">Settings</a>' );
		}
		return $links;

	}

	 // Register valid admin options...
	function syndicate_out_register_settings() {
	
		register_setting( 'syndicate-out-options', 'so_options', 'syndicate_out_sanitize_options' );

	}

	 // Admin page...
	function syndicate_out_admin() {
	
		if ( false === ( $syndicateOutOptions = get_option( 'so_options' ) ) ) {
			$syndicateOutOptions['group'][] = array( 'category' => null,
			                                         'syndicate_category' => 'none',
			                                         'servers' => array( array( 'server' => '',
			                                                                    'username' => '',
			                                                                    'password' => '' ) ) );
		}
		$newServerRows = get_transient( 'so_new_servers' );
		$newGroupRows = get_transient( 'so_new_groups' );

		require_once( 'so-options.php' );

	}

 /* Post / action section. */
 
	 // Sanitize and organise the all settings...
	function syndicate_out_sanitize_options( $options ) {
	
		if ( ! isset( $options['options_version'] ) ) {
	 // Delete any groups which have been flagged for deletion...
			if ( isset( $options['deletegroup'] ) ) {
				foreach ( $options['deletegroup'] AS $groupKey => $buttonValue ) {
					if ( array_key_exists( $groupKey, $options ) ) {
						unset( $options[$groupKey] );
					}
				}
				unset( $options['deletegroup'] );
			}
		
	 // Save all group settings...
			$addRowsArray = array();
			$newOptions = array( 'group' => array() );
			if ( isset( $options['group'] ) && is_array( $options['group'] ) ) {
				foreach ( $options['group'] AS $groupId => $groupOptions ) {
				
	 // If this group isn't flagged for deletion...
					if ( ! isset( $groupOptions['deletegroup'] ) ) {
				
	 // Flag new rows, if required...
						if ( isset( $groupOptions['addrowbutton'] ) && is_numeric( $groupOptions['addrow'] ) && $groupOptions['addrow'] > 0 ) {
							$addRowsArray[$groupId] = $groupOptions['addrow'];
						}

	 // Triggers and trigger category...
						switch ( $groupOptions['trigger'] ) {
							case 'all':
								$newOptions['group'][$groupId]['category'] = -1;
							break;
							case 'category':
								if ( is_numeric( $groupOptions['category'] ) ) {
									$newOptions['group'][$groupId]['category'] = $groupOptions['category'];
									break;
								}
							case 'disable': default:
								$newOptions['group'][$groupId]['category'] = 'none';
							break;
						}

	 // Transmit category...
						switch ( $groupOptions['syndicate_category'] ) {
							case 'all': case 'syndication':
								$newOptions['group'][$groupId]['syndicate_category'] = $groupOptions['syndicate_category'];
							break;
							default:
								$newOptions['group'][$groupId]['syndicate_category'] = 'none';
							break;
						}

	 // Servers...
						foreach ( $groupOptions['servers'] AS $serverKey => $serverDetails ) {
							if ( ! empty( $serverDetails['server'] ) ) {
								$remoteServer = trim( $serverDetails['server'] );
								if ( 'http://' != substr( $remoteServer, 0, 7 ) ) {
						         $remoteServer = 'http://'.$remoteServer;
								}
								if ( '/' != substr( $remoteServer, -1 ) ) {
						         $remoteServer .= '/';
								}
								$newOptions['group'][$groupId]['servers'][$serverKey]['server'] = $remoteServer;
								$newOptions['group'][$groupId]['servers'][$serverKey]['username'] = $serverDetails['username'];
								$newOptions['group'][$groupId]['servers'][$serverKey]['password'] = $serverDetails['password'];
							}
						}
					
					}
					
				}
			}
			
	 // Set the transient relating to new server rows...
			if ( count( $addRowsArray ) > 0 ) {
				set_transient( 'so_new_servers', $addRowsArray, 5 );
			}
			
	 // Set the transient relating to new groups...
			if ( isset( $options['addgroupbutton'] ) && is_numeric( $options['addgroup'] ) && $options['addgroup'] > 0 ) {
				set_transient( 'so_new_groups', $options['addgroup'], 5 );
			}

	 // Grab the old settings...
			$oldOptions = get_option( 'so_options' );
			if ( isset( $oldOptions['options_version'] ) ) {
				$newOptions['options_version'] = $oldOptions['options_version'];
			} else {
				$newOptions['options_version'] = SO_OPTIONS_VERSION;
			}
			
			return $newOptions;
		} else {
			return $options;
		}

	}

/*
function output_base64() {
$fh = fopen('Input-File', 'rb'); 
//$fh2 = fopen('Output-File', 'wb'); 

$cache = ''; 
$eof = false; 

while (1) { 

    if (!$eof) { 
        if (!feof($fh)) { 
            $row = fgets($fh, 4096); 
        } else { 
            $row = ''; 
            $eof = true; 
        } 
    } 

    if ($cache !== '') 
        $row = $cache.$row; 
    elseif ($eof) 
        break; 

    $b64 = base64_encode($row); 
    $put = ''; 

    if (strlen($b64) < 76) { 
        if ($eof) { 
            $put = $b64."\n"; 
            $cache = ''; 
        } else { 
            $cache = $row; 
        } 

    } elseif (strlen($b64) > 76) { 
        do { 
            $put .= substr($b64, 0, 76)."\n"; 
            $b64 = substr($b64, 76); 
        } while (strlen($b64) > 76); 

        $cache = base64_decode($b64); 

    } else { 
        if (!$eof && $b64{75} == '=') { 
            $cache = $row; 
        } else { 
            $put = $b64."\n"; 
            $cache = ''; 
        } 
    } 

    if ($put !== '') { 
        echo $put; 
        //fputs($fh2, $put); 
        //fputs($fh2, base64_decode($put));        // for comparing 
    } 
} 

//fclose($fh2); 
fclose($fh); 
}
*/

	 // Carry out the syndication on post insert...
	function syndicate_out_post( $postId ) {
	
		if ( $soOptions = get_option( 'so_options' ) ) {
			if ( isset( $soOptions['group'] ) && is_array( $soOptions['group'] ) ) {
			
				foreach ( $soOptions['group'] AS $syndicationGroup ) {
					if ( ( -1 == $syndicationGroup['category'] ) || in_category( $syndicationGroup['category'], $postId ) ) {
						$activeGroups[] = $syndicationGroup;
					}
				}
				
				if ( count( $activeGroups ) > 0 ) {
					if ( @include_once(  ABSPATH . WPINC . '/class-IXR.php' ) ) {
		
	 // Get required post information...
						$postData = get_post( $postId );
/////////////////////////////////////
/*
   $postattachments_args = array(
	'post_type' => 'attachment',
   	'numberposts' => -1,
   	'post_status' => null,
   	'post_parent' => $postId
   );

  $post_attachments = get_posts( $postattachments_args );
     if ( $post_attachments ) {
        foreach ( $post_attachments as $post_attachment ) {
          get_attached_file( $post_attachment->ID, 'full' );
          }
     }
*/
/////////////////////////////////////
						if ( 'inherit' == $postData->post_status ) {
							$postMetaId = $postData->post_parent;
						} else {
							$postMetaId = $postId;
						}
		
	 // Title...
						$remotePost['title'] = $postData->post_title;
	 // Description...
						$remotePost['description'] = $postData->post_content;
	 // Permalink...
						$remotePost['link'] = get_permalink( $postData->ID );

	 // Custom fields...
						$postMeta = has_meta( $postMetaId );
						if ( is_array( $postMeta ) ) {
							$remotePost['custom_fields'] = array();
							foreach ( $postMeta AS $metaSingle ) {
								if ( $metaSingle['meta_key'][0] != '_' ) {
									$remotePost['custom_fields'][] = array( 'key' => $metaSingle['meta_key'],
									                                        'value' => $metaSingle['meta_value'] );
								}
							}
						}
							
	 // Tags...
						if ( $postTags = syndicate_out_get_tags( $postId ) ) {
							$keywords = array();
							foreach ( $postTags AS $postTag ) {
								$keywords[] = $postTag->name;
							}
							$remotePost['mt_keywords'] = implode( ',', $keywords );
						}
							
	 // Categories...
						$groupCategoryArray = array();
						foreach ( $activeGroups AS $groupKey => $groupDetails ) {
							if ( 'none' != $groupDetails['syndicate_category'] ) {
								if ( 'syndication' == $groupDetails['syndicate_category'] && ( -1 != $syndicationGroup['category'] ) ) {
									$groupCategoryArray[$groupKey]['categories'] = array( get_cat_name( $groupDetails['category'] ) );
								} else if ( ( 'all' == $groupDetails['syndicate_category'] ) || ( -1 == $syndicationGroup['category'] ) ) {
									$categories = get_the_category( $postId );
									$groupCategoryArray[$groupKey]['categories'] = array();
									foreach ( $categories AS $postCategory ) {
										$groupCategoryArray[$groupKey]['categories'][] = $postCategory->cat_name;
									}
								}
							}
						}

	 // Publish the post to the remote blog(s)...
						if ( false !== ( $remotePostIds = unserialize( get_post_meta( $postMetaId, '_so_remote_posts', true ) ) ) ) {
							if ( ! isset( $remotePostIds['options_version'] ) ) {
								$newRemotePostIds = array( 'options_version' => SO_OPTIONS_VERSION );
								foreach ( $remotePostIds AS $serverKey => $remotePostId ) {
									$newRemotePostIds['group'][0][$serverKey] = $remotePostId;
								}
								$remotePostIds = $newRemotePostIds;
								update_post_meta( $postMetaId, '_so_remote_posts', serialize( $remotePostIds ) );
							}
							foreach ( $remotePostIds['group'] AS $groupKey => $remoteServers ) {
								if ( isset( $groupCategoryArray[$groupKey] ) ) {
									$compiledGroupPost = array_merge( $remotePost, $groupCategoryArray[$groupKey] );
								} else {
									$compiledGroupPost = $remotePost;
								}
								foreach ( $remoteServers AS $serverKey => $remotePostId ) {
									if ( is_numeric( $remotePostId ) ) {
										if ( isset( $soOptions['group'][$groupKey]['servers'][$serverKey] ) ) {
											$xmlrpc = new IXR_Client( $soOptions['group'][$groupKey]['servers'][$serverKey]['server'].'xmlrpc.php' );
											$xmlrpc->query( 'metaWeblog.editPost', $remotePostId, $soOptions['group'][$groupKey]['servers'][$serverKey]['username'], $soOptions['group'][$groupKey]['servers'][$serverKey]['password'], $compiledGroupPost, 1 );
										}
									}
								}
							}
						} else {
							$remotePostInformation = array( 'options_version' => SO_OPTIONS_VERSION );
							foreach ( $activeGroups AS $groupKey => $activeGroup ) {
								if ( isset( $groupCategoryArray[$groupKey] ) ) {
									$compiledGroupPost = array_merge( $remotePost, $groupCategoryArray[$groupKey] );
								} else {
									$compiledGroupPost = $remotePost;
								}
								foreach ( $activeGroup['servers'] AS $serverKey => $serverDetails ) {
									$xmlrpc = new IXR_Client( $serverDetails['server'].'xmlrpc.php' );

									$xmlrpc->query( 'metaWeblog.newPost', 1, $serverDetails['username'], $serverDetails['password'], $compiledGroupPost, 1 );
									$remotePostInformation['group'][$groupKey][$serverKey] = $xmlrpc->getResponse();
/////////////////////
   $postattachments_args = array(
       'post_type' => 'attachment',
        'numberposts' => -1,
        'post_status' => null,
        'post_parent' => $postId
   );					

     $post_attachments = get_posts( $postattachments_args );
     if ( $post_attachments ) {
	foreach ( $post_attachments as $post_attachment ) {
          $filepath = get_attached_file( $post_attachment->ID );

  	error_log("going to post: " . $filepath);
	  $fs = filesize($filepath);
	  $file = fopen($filepath, 'rb');
	  $filedata = fread($file, $fs);
	  fclose($file);
	 
   $attachment_post = array(
        'post_type' => 'attachment',
        'post_parent' => $remotePostInformation['group'][$groupKey][$serverKey],
	'post_mime_type' => 'image/jpeg'
   );

		$xmlrpc->query( 'metaWeblog.newPost', 1, $serverDetails['username'], $serverDetails['password'], $attachment_post, 1);
		$response = $xmlrpc->getResponse();
	error_log("response: " . $response);
	for ($r = 0; $r < sizeof($response); $r++) {
		error_log($response[$r]);
	}
	
        }
     }
////////////////////									
									
								}
							}
							update_post_meta( $postMetaId, '_so_remote_posts', serialize( $remotePostInformation ) );
						}
						
					}
				}
			
			}
		}

	}

	 // Get a list of tags for this post...
	function syndicate_out_get_tags( $postId ) {

		$terms = get_object_term_cache( $postId, 'post_tag' );
		if ( false === $terms ) {
			$terms = wp_get_object_terms( $postId, 'post_tag' );
		}

		if ( empty( $terms ) ) {
			return false;
		}

		return $terms;

	}

 /* Maintenance section. */

	 // Activation function.  Updates the old settings storage format to the
	 // new settings storage format...
	function syndicate_out_activate() {

		if ( $oldSettingsCategory = get_option( 'so_category' ) ) {
			$oldSettingsServer = get_option( 'so_remote_server' );
			$oldSettingsUsername = get_option( 'so_remote_username' );
			$oldSettingsPassword = get_option( 'so_remote_password' );
			$newSettings = array( 'group' => array( array( 'category' => $oldSettingsCategory,
			                                               'syndicate_category' => 'none',
			                                               'servers' => array( array( 'server' => $oldSettingsServer,
			                                                                          'username' => $oldSettingsUsername,
			                                                                          'password' => $oldSettingsPassword ) ) ) ),
			                      'options_version' => SO_OPTIONS_VERSION );
			update_option( 'so_options', $newSettings );
			delete_option( 'so_category' );
			delete_option( 'so_remote_server' );
			delete_option( 'so_remote_username' );
			delete_option( 'so_remote_password' );

		} else if ( $oldSettings = get_option( 'so_options' ) ) {
		
			if ( isset( $oldSettings['options_version'] ) ) {
				$optionsVersion = $oldSettings['options_version'];
				unset( $oldSettings['options_version'] );
			} else {
				$optionsVersion = 0;
			}
			switch ( $optionsVersion ) {
				case 0: case 1:
					$newSettings['group'][0] = $oldSettings;
				break;
			}
			$newSettings['options_version'] = SO_OPTIONS_VERSION;
			update_option( 'so_options', $newSettings );
			
		}

	}

}
