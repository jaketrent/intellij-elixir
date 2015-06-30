// This is a generated file. Not intended for manual editing.
package org.elixir_lang.psi;

import com.ericsson.otp.erlang.OtpErlangObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ElixirAdjacentExpression extends Quotable {

  @Nullable
  ElixirEmptyParentheses getEmptyParentheses();

  @Nullable
  ElixirUnmatchedExpression getUnmatchedExpression();

  @Nullable
  ElixirUnqualifiedNoParenthesesManyArgumentsCall getUnqualifiedNoParenthesesManyArgumentsCall();

  @NotNull
  OtpErlangObject quote();

}
