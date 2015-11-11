class PostSerializer < ApplicationSerializer
  attributes :title, :body, :likes, :author_id
end
