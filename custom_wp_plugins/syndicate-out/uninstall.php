<?php

	 // If this file is called during an uninstall, we want to carry on...
if ( defined( 'WP_UNINSTALL_PLUGIN' ) ) {

	 // Firstly delete the general options...
	delete_option( 'so_options' );
	 // And then delete all syndication data from the posts...
	if ( $allPosts = get_posts(array('meta_key' => '_so_remote_posts')) ) {
		foreach ( $allPosts AS $postDetails ) {
			delete_post_meta( $postDetails->ID, '_so_remote_posts' );
		}
	}
	
}