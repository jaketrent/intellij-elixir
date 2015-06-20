// This is a generated file. Not intended for manual editing.
package org.elixir_lang.psi;

import com.ericsson.otp.erlang.OtpErlangObject;
import org.jetbrains.annotations.NotNull;

public interface ElixirEmptyParenthesesHatOperation extends ElixirEmptyParenthesesExpression, InfixOperation {

  @NotNull
  ElixirEmptyParentheses getEmptyParentheses();

  @NotNull
  ElixirEmptyParenthesesExpression getEmptyParenthesesExpression();

  @NotNull
  ElixirHatInfixOperator getHatInfixOperator();

  @NotNull
  OtpErlangObject quote();

}
