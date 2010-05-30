package jess;

class TokenHolder
{
  Token m_token;
  TokenHolder m_right, m_left;
  int m_code;

  TokenHolder(int code)
  {
    m_code = code;
  }
}
