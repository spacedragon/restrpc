package io.insight.http;

import com.google.common.collect.Multimap;

public interface HttpRequest {
  Multimap<String, String> headers();
}
