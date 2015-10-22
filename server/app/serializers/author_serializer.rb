class AuthorSerializer < ApplicationSerializer
  attributes :name, :email, :avatar

  # has_many :posts
end
