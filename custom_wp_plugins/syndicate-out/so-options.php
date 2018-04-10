<script type="text/javascript">
	function toggleIgnoreServer(groupid, serverid, checkboxStatus) {
		var disabledcolor = '#A0A0A0';
		var serverserverinput = document.getElementById('serverserver-' + groupid + '-' + serverid);
		var serverusernameinput = document.getElementById('serverusername-' + groupid + '-' + serverid);
		var serverpasswordinput = document.getElementById('serverpassword-' + groupid + '-' + serverid);
		if (checkboxStatus == true) {
			serverserverinput.disabled = true; serverserverinput.style.color = disabledcolor;
			serverusernameinput.disabled = true; serverusernameinput.style.color = disabledcolor;
			serverpasswordinput.disabled = true; serverpasswordinput.style.color = disabledcolor;
		} else {
			serverserverinput.disabled = false; serverserverinput.style.color = '';
			serverusernameinput.disabled = false; serverusernameinput.style.color = '';
			serverpasswordinput.disabled = false; serverpasswordinput.style.color = '';
		}
	}
	function toggleTriggerCategory(groupid, triggervalue) {
		if (triggervalue == 'category') {
			document.getElementById('triggercategory-' + groupid).disabled = false;
		} else {
			document.getElementById('triggercategory-' + groupid).disabled = true;
		}
	}
</script>
<div class="wrap">
	<h2>Syndicate Out</h2>
	<form method="post" action="options.php">
	<?php settings_fields( 'syndicate-out-options' ); ?>
		<p>Each category may be syndicated to as many blogs as required, and a remote blog may appear in as many groups as needed.  Posts which match multiple groups with duplicate servers will only be syndicated to the remote blog once but will use the least restrictive 'transmit categories' setting.  There is no limit to the number of groups which may be added.</p>

<?php
	if ( isset( $syndicateOutOptions['group'] ) && is_array( $syndicateOutOptions['group'] ) ) {
		if ( $newGroupRows > 0 ) {
			$syndicateOutOptions['group'][] = array();
		}
		foreach ( $syndicateOutOptions['group'] AS $groupKey => $syndicationGroup ) {
			$additionalRows = 0;
			if ( is_array( $newServerRows ) && count( $newServerRows ) > 0 ) {
				if ( array_key_exists( $groupKey, $newServerRows ) && $newServerRows[$groupKey] > 0 ) {
					$additionalRows = $newServerRows[$groupKey];
				}
			}
?>
		<div style="padding-bottom: 15px;">
			<h3>Syndication Group <?php echo ($groupKey + 1); ?></h3>
			
			<table class="form-table">
				<tbody>
					<tr>
					   <th scope="row">Syndicate</th>
					   <td>
							<select id="triggermethod-<?php echo $groupKey; ?>" name="so_options[group][<?php echo $groupKey ?>][trigger]" onchange="toggleTriggerCategory(<?php echo $groupKey ?>, this.value);">
								<option value="disable"<?php echo ( 'none' == $syndicationGroup['category'] ) ? ' selected="selected"' : ''; ?>>Disabled</option>
								<option value="all"<?php echo ( -1 == $syndicationGroup['category'] ) ? ' selected="selected"' : ''; ?>>All posts</option>
								<option value="category"<?php echo ( 0 < $syndicationGroup['category'] ) ? ' selected="selected"' : ''; ?>>Selected category</option>
								<!--<option value="post">Selected posts</option>-->
							</select>
							<select id="triggercategory-<?php echo $groupKey; ?>" name="so_options[group][<?php echo $groupKey ?>][category]" <?php if ( -1 == $syndicationGroup['category'] || 'none' == $syndicationGroup['category'] ) { echo 'disabled="true"'; } ?>>
								<option value="-1">Select category</option>
<?php
			foreach ( get_categories( array ( 'hide_empty' => 0 ) ) AS $blogCategory ) {
				echo '<option value="'.$blogCategory->cat_ID.'"'.( ( $syndicationGroup['category'] == $blogCategory->cat_ID ) ? ' selected="selected"' : '' ).'>'.$blogCategory->cat_name.'</option>';
			}
?>
							</select>
						</td>
					</tr>
					<tr valign="top">
						<th scope="row">Transmit categories</th>
						<td>
							<select id="triggermethod-<?php echo $groupKey; ?>" name="so_options[group][<?php echo $groupKey ?>][syndicate_category]">
								<option value="none"<?php echo ( 'none' == $syndicationGroup['syndicate_category'] ) ? ' selected="selected"' : ''; ?>>No categories</option>
								<option value="all"<?php echo ( 'all' == $syndicationGroup['syndicate_category'] ) ? ' selected="selected"' : ''; ?>>All categories</option>
								<option value="syndication"<?php echo ( 'syndication' == $syndicationGroup['syndicate_category'] ) ? ' selected="selected"' : ''; ?>>Syndication category only</option>
							</select>
						</td>
					</tr>
				</tbody>
			</table>
			
			<br />
			<table class="widefat">
				<thead>
				<tr valign="top">
					<th scope="column" class="manage-column check-column"></th>
					<th scope="column" class="manage-column">Address</th>
					<th scope="column" class="manage-column">Username</th>
					<th scope="column" class="manage-column">Password</th>
					<th scope="column" class="manage-column">Status</th>
				</tr>
				</thead>
				<tbody>
<?php
			if ( count( $syndicationGroup['servers'] ) == 0 || isset( $_POST['addrow'][$groupKey] ) ) {
				$syndicationGroup['servers'][] = array( 'server' => '', 'username' => '', 'password' => '' );
			}
			if ( $additionalRows > 0 ) {
				for ($i = 0; $i < $additionalRows; $i++) {
					$syndicationGroup['servers'][] = array( 'server' => '', 'username' => '', 'password' => '' );
				}
			}
			foreach ( $syndicationGroup['servers'] AS $serverKey => $soServer ) {
?>
					<tr id="serverrow-<?php echo $serverKey ?>-<?php echo $groupKey ?>">
						<th scope="row" class="check-column"><input type="checkbox" name="so_options[group][<?php echo $groupKey ?>][servers][<?php echo htmlentities2( $serverKey ); ?>][delete]" value="1" onclick="toggleIgnoreServer(<?php echo $serverKey ?>, <?php echo $groupKey ?>, this.checked);" /></th>
						<td><input id="serverserver-<?php echo $serverKey ?>-<?php echo $groupKey ?>" style="width: 260px;" type="text" name="so_options[group][<?php echo $groupKey ?>][servers][<?php echo htmlentities2( $serverKey ); ?>][server]" value="<?php echo htmlentities2( $soServer['server'] ); ?>" /></td>
						<td><input id="serverusername-<?php echo $serverKey ?>-<?php echo $groupKey ?>" type="text" name="so_options[group][<?php echo $groupKey ?>][servers][<?php echo htmlentities2( $serverKey ); ?>][username]" value="<?php echo htmlentities2( $soServer['username'] ); ?>" /></td>
						<td><input id="serverpassword-<?php echo $serverKey ?>-<?php echo $groupKey ?>" type="password" name="so_options[group][<?php echo $groupKey ?>][servers][<?php echo htmlentities2( $serverKey ); ?>][password]" value="<?php echo htmlentities2( $soServer['password'] ); ?>" /></td>
						<!--<td>Authentication: <span style="color: #006505;">OK</span> <span style="color: #BC0B0B;">failed</span>.<br />Remote API: <span style="color: #D98500;">WordPress default</span>.</td>-->
						<td>Authentication: <span style="color: #777777;">unknown</span>.<br />Remote API: <span style="color: #777777;">unknown</span>.</td>
					</tr>
<?php
			}
?>
				</tbody>
			</table>
			<div class="tablenav bottom">
				<div class="alignleft actions">
					Add <input type="text" size="3" value="1" name="so_options[group][<?php echo $groupKey; ?>][addrow]" /> new server(s) to group <input type="submit" name="so_options[group][<?php echo $groupKey; ?>][addrowbutton]" value="Go" class="button" />
				</div>
				<div class="alignleft actions">
					<input type="submit" name="so_options[group][<?php echo $groupKey; ?>][deletegroup]" value="Delete group" class="button delete" />
				</div>
				<div class="tablenav-pages one-page">
					<span class="displaying-num"><?php echo count( $syndicationGroup['servers'] ) ; ?> <?php echo ( count( $syndicationGroup['servers'] ) == 1 ) ? 'server' : 'servers'; ?></span>
				</div>
			</div>
			
		</div>
<?php
		}
	}
?>
		<p class="submit">
			Add <input type="text" size="3" value="1" name="so_options[addgroup]" /> new group(s) <input type="submit" name="so_options[addgroupbutton]" value="Go" class="button" />
			<input type="submit" name="save" class="button-primary" value="<?php _e( 'Save settings' ) ?>" />
		</p>
	</form>
</div>