// This is a generated file. Not intended for manual editing.
package org.elixir_lang.psi;

import com.ericsson.otp.erlang.OtpErlangObject;
import org.jetbrains.annotations.NotNull;

public interface ElixirAtBlockOperation extends PrefixOperation {

  @NotNull
  ElixirAtPrefixOperator getAtPrefixOperator();

  @NotNull
  ElixirBlockExpression getBlockExpression();

  @NotNull
  OtpErlangObject quote();

}