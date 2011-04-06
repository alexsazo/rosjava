/*
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Willow Garage, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ros.android.activity;

import android.widget.BaseAdapter;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
import java.util.ArrayList;

public class MasterAdapter extends BaseAdapter {
  private Context context_;
  private List<String> master_uris_;
  private String my_hostname_;
  private List<MasterItem> master_items_;

  public MasterAdapter(Context c, List<String> master_uris, String my_hostname ) {
    context_ = c;
    master_uris_ = master_uris;
    my_hostname_ = my_hostname;
    master_items_ = new ArrayList<MasterItem>();
    for( int i = 0; i < master_uris_.size(); i++ )
    {
      master_items_.add( new MasterItem( master_uris_.get( i ), my_hostname_ ));
    }
  }

  public int getCount() {
    if( master_uris_ == null )
    {
      return 0;
    }
    return master_uris_.size();
  }

  public boolean areAllItemsEnabled() {
    return false;
  }

  public boolean isEnabled( int position ) {
    return master_items_.get( position ).isOk();
  }

  public Object getItem(int position) {
    return null;
  }

  public long getItemId(int position) {
    return 0;
  }

  // create a new View for each item referenced by the Adapter
  public View getView(int position, View convertView, ViewGroup parent) {
    return master_items_.get( position ).getView( context_, convertView, parent );
  }
}
