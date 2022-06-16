module.exports = {
  // extends: ['@commitlint/config-conventional'],
  rules: {
    'change-id': [2, 'always'],
    'issue-num': [2, 'always']
  },
  plugins: [
    {
      rules: {
        'change-id': ({ body }) => {
          const changeReg = /Change\-Id\:\sI/m
          return [changeReg.exec(body) && true, `Your subject should contain Change-Id`]
        },
        'issue-num': ({ subject }) => {
          const issueReg = /\#\d+/m
          return [issueReg.exec(subject) && true, `Your subject should contain a specific issue`]
        }
      }
    }
  ]
}
