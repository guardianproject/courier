=== Plugin Name ===
Contributors: ConfuzzledDuck
Tags: syndication, xmlrpc, cross-post, post, content, autoblogging
Requires at least: 2.7.0
Tested up to: 3.5
Stable tag: 0.7

Syndicates posts made in any specified category to another WordPress blog using WordPress' built in XMLRPC functionality.

== Description ==

Syndicate Out syndicates all posts made in a specified category (or all posts on a blog) to any other WordPress
blog(s) in real time. This enables blog owners to create automatic aggregating, or topic specific blogs from any
number of different blog sources without relying on RSS feeds or any kind of timed events.

The plugin uses WordPress' built in XMLRPC functionality to push posts to the target blog.  XML-RPC will need
to be enabled on the receiving blog in order for this plugin to work. See the installation section for more
details.

More information is available, feature requests and bug reports are gladly accepted over on the
[project's own blog](http://www.flutt.co.uk/development/wordpress-plugins/syndicate-out/).

== Installation ==

1. Set up a user on the target blog to receive the posts
1. Switch on XML-RPC remote publishing on the remote blog (not required since WordPress 3.5)
1. Upload the syndicate-out directory to the `/wp-content/plugins/` directory
1. Activate the plugin through the 'Plugins' menu in WordPress
1. Configure the plugin through the 'Settings'->'Syndication' menu

You may then test the plugin by posting to the selected syndication category.
The post should immediately be cross-posted to the remote blog.

== Changelog ==

= 0.7 =

* Updates and testing for WordPress up to 3.5.
* Added settings link to plugin page.
* Overhauled look of the settings page.
* Added syndication of permalink.

= 0.6 =

* Added custom field syndication functionality.
* Added delete operations to tidy up all plugin data on uninstall.
* Fixed major bug in syndicating all categories.
* Re-worked sections of options sanitation to fix a bug on first save (thanks Kevin).
* Reworked the decision making logic around new posts / edit posts.
* Corrected some malformed markup in settings page.

= 0.5.1 =

* Bugfix to settings page category dropdown introduced in 0.5.

= 0.5 =

* Added syndication groups.
* Added the ability to syndicate all posts on a blog, not just one category (thanks jc).
* Added versioning to stored options to cater for future upgrades.
* Fixed a settings array related bug (thanks Dan, TJ).
* Re-housed a lonely PHP short tag (thanks TJ).

= 0.4.1 =

* Fixed a bug which was causing warnings to be issued in some circumstances (thanks Dan).

= 0.4 =

* Added ability to syndicate to multiple blogs (thanks Chris, Cat, Danel).
* Added ability to send category information with the syndicated post (thanks Martin).
* Posts which have already been syndicated will be syndicated from the source blog if they're edited.
* Modified the storage format of so_options to handle new functionality.

= 0.3.2 =

* Fixed so-options include bug (thanks Paul Bain).
* Modified permission levels for admin page to hopefully fix visibility bug (thanks randy, Adam and Paul).

= 0.3.1 =

* Changed IXR include line to use ABSPATH and WPINC.
* Modified handling of edited posts so they don't get re-posted on the remote blog.

= 0.3 =

* Fixed IXR include bug.
* Added tag handling (they are now passed on to the remote blog).

= 0.2 =

* First available public release.
